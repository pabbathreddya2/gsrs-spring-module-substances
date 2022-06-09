package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.module.substance.repository.SubstanceRepository;
import gsrs.module.substance.utils.NameStandardizer;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import ix.core.utils.executor.ProcessListener;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
public class NameStandardizerTaskInitializer extends ScheduledTaskInitializer {

    private String nameStandardizerClassName = null;
    private String regenerateNameValue = "";

    @Autowired
    private SubstanceRepository substanceRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    public NameStandardizerTaskInitializer() {
    }

    private final Integer PAGE_SIZE=200;
    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        if( this.nameStandardizerClassName== null || this.nameStandardizerClassName.length()==0) {
            this.nameStandardizerClassName="gsrs.module.substance.utils.FDAFullNameStandardardizer";
        }

        log.trace("Going to instantiate standardizer with name {}", this.nameStandardizerClassName);
        NameStandardizer nameStandardizer;
        try {
            nameStandardizer = (NameStandardizer) Class.forName(this.nameStandardizerClassName).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.error(e.getMessage());
            log.error("cannot perform standardization; no standardizer");
            return;
        }
        l.message("Initializing standardization");
        log.trace("Initializing standardization");
        //List<Substance> substanceList =
        long totalSubstances =substanceRepository.count();

        ProcessListener listen = ProcessListener.onCountChange((sofar, total) -> {
            if (total != null) {
                l.message("Generated standard names for :" + sofar + " of " + total);
            } else {
                l.message("Generated standard names for :" + sofar);
            }
        });

        l.message("Initializing name standardization: acquiring list");

        listen.newProcess();
        listen.totalRecordsToProcess((int)totalSubstances);
        log.trace("got list. size: {}", totalSubstances);

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing name standardization: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing name standardization: starting process");
        log.trace("starting process");

        try {
            adminService.runAs(adminAuth, (Runnable) () -> {
                TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                log.trace("got tx " + tx);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    processInBackground(substanceRepository, nameStandardizer);
/*
                    log.trace("before substanceRepository.streamAll. substanceRepository: " + substanceRepository);
                    substanceRepository.streamAll().forEach(substance -> {
                        log.trace("processing substance " + substance.getUuid());
                        try {
                            tx.executeWithoutResult(status -> {
                                log.trace("tx.executeWithoutResult");
                                if(standardizeNamesForSubstance(substance, nameStandardizer) ) {
                                    log.trace("resaving substance "+ substance.getUuid());
                                    substanceRepository.saveAndFlush(substance);
                                }
                            });
                        } catch (Throwable ex) {
                            log.error("error standardizing names", ex);
                            l.message("Error standardizing names for " + substance.getUuid().toString() + "; error: "
                                    + ex.getMessage());
                        }
                    });
*/
                } catch (Exception ex) {
                    l.message("Error standardizing. error: " + ex.getMessage());
                }
            });
        } catch (Exception ee) {
            log.error("error generating standard names: ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        l.message("Shutting down executor service");
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            //should never happen
            log.error("Interrupted exception!");
        }
        l.message("Task finished");
        listen.doneProcess();

    }

    @Override
    public String getDescription() {
        return "Regenerate standardized names for all substances in the database";
    }

    public void setNameStandardizerClassName(String nameStandardizerClassName) {
        this.nameStandardizerClassName = nameStandardizerClassName;
    }

    public void setRegenerateNameValue(String regenerateNameValue) {
        this.regenerateNameValue = regenerateNameValue;
    }

    private boolean standardizeNamesForSubstance(Substance substance, NameStandardizer nameStandardizer){

        log.trace("starting in standardizeNamesForSubstance");
        AtomicBoolean nameChanged = new AtomicBoolean(false);
        try {
            Substance fullSubstance= substanceRepository.getOne(substance.uuid);
            fullSubstance.names.forEach((Name name) -> {
                log.trace("in StandardNameValidator, Name " + name.name);

                if (name.stdName != null && name.stdName.equals(regenerateNameValue)) {
                    name.stdName = null;
                }

                if (name.stdName == null) {
                    name.stdName = nameStandardizer.standardize(name.name).getResult();
                    log.debug("set (previously null) stdName to " + name.stdName);
                    nameChanged.set(true);
                }
            });

        } catch (Exception ex) {
            log.error("Error processing names");
            log.error(ex.getMessage());
            ex.printStackTrace();
        }
        return nameChanged.get();
    }

    private void processInBackground(SubstanceRepository repository, NameStandardizer nameStandardizer){
        log.trace("starting in processInBackground");
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Pageable currentPageable = pageable;

        while(currentPageable != null) {
            Page page = repository.findAll(currentPageable);
            log.trace("got page");
            Stream<Substance> substanceStream = page.stream();

            substanceStream.forEach( sub->{
                log.trace("processing sub {}", sub.uuid.toString());
                if(standardizeNamesForSubstance(sub, nameStandardizer) ) {
                    log.trace("resaving substance "+ sub.getUuid());
                    substanceRepository.saveAndFlush(sub);
                }

            });
            if(page.hasNext()){
                currentPageable = page.nextPageable();
            }else{
                currentPageable=null;
            }

        }

    }
}
