/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.tests.packaging.beans.xml.location;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.BeansXml;
import org.jboss.weld.tests.category.Integration;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Tests that "WEB-INF/beans.xml" takes precedence over "WEB-INF/classes/META-INF/beans.xml" if both locations are used. This
 * is not required by the specification and is Weld-specific.
 *
 * @author Jozef Hartinger
 *
 */
@RunWith(Arquillian.class)
@Category(Integration.class)
public class MultipleLocationsTest extends AbstractBeansXmlLocationTest {

    @Deployment
    public static WebArchive getDeployment() {
        Asset beans = getBeansXml();
        return getBaseDeployment().addAsWebInfResource(beans, "beans.xml").addAsResource(new BeansXml(), "META-INF/beans.xml");
    }
}
