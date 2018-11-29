package org.jboss.weld.tests.jsf.weld2548;

import java.net.URL;

import com.gargoylesoftware.htmlunit.WebClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.weld.test.util.Utils;
import org.jboss.weld.tests.category.Integration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category(Integration.class)
@RunWith(Arquillian.class)
public class Weld2548Test {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, Utils.getDeploymentNameAsHash(Weld2548Test.class, Utils.ARCHIVE_TYPE.WAR))
                .addClass(App.class)
                .addAsWebResource(Weld2548Test.class.getPackage(), "index.xhtml", "index.xhtml")
                .addAsWebInfResource(Weld2548Test.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(Weld2548Test.class.getPackage(), "faces-config.xml", "faces-config.xml");
    }

    @Test
    @RunAsClient
    public void openIndexPage(@ArquillianResource URL url) {
        WebClient client = new WebClient();
        try {
            client.getPage(url + "/index.xhtml");
            Assert.fail("Exception expected");
        } catch (Exception e) {
            Assert.assertTrue("Exception expected", true);
        }
    }

}
