package org.jboss.weld.environment.servlet.test.deployment.structure;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.util.ArrayList;
import java.util.List;

public class ContainerLifecycleObserver implements Extension {

    private List<AnnotatedType<?>> processedAnnotatedTypes;

    public ContainerLifecycleObserver() {
        processedAnnotatedTypes = new ArrayList<AnnotatedType<?>>();
    }

    public void observeProcessFoo(@Observes ProcessAnnotatedType<? extends Foo> event) {
        this.processedAnnotatedTypes.add(event.getAnnotatedType());
    }

    public void observeProcessBaz(@Observes ProcessAnnotatedType<? extends Baz> event) {
        this.processedAnnotatedTypes.add(event.getAnnotatedType());
    }

    public List<AnnotatedType<?>> getProcessedAnnotatedTypes() {
        return processedAnnotatedTypes;
    }

}
