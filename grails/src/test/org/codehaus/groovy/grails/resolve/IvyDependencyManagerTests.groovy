package org.codehaus.groovy.grails.resolve

import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.util.Message
import org.apache.ivy.util.MessageLogger
import org.apache.ivy.util.DefaultMessageLogger
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.GrailsUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class IvyDependencyManagerTests extends GroovyTestCase{

    protected void setUp() {
        System.metaClass.static.getenv = { String name -> "." }
    }

    protected void tearDown() {
        GroovySystem.metaClassRegistry.removeMetaClass(System) 
    }

    void testMapSyntaxForDependencies() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            runtime( [group:"opensymphony", name:"oscache", version:"2.4.1", transitive:false],
                     [group:"junit", name:"junit", version:"3.8.2", transitive:true] )
        }


        assertEquals 2, manager.listDependencies('runtime').size()
    }

    void testDefaultDependencyDefinition() {

        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def manager = new IvyDependencyManager("test", "0.1")
        def props = new Properties()
        new File("./build.properties").withInputStream {
            props.load(it)
        }


        manager.parseDependencies(IvyDependencyManager.getDefaultDependencies(props.'grails.version'))
        assertEquals 52, manager.listDependencies('runtime').size()
        assertEquals 56, manager.listDependencies('test').size()
        assertEquals 17, manager.listDependencies('build').size()
        assertEquals 3, manager.listDependencies('provided').size()
        def report = manager.resolveDependencies()


        assertFalse "dependency resolve should have no errors!",report.hasError()
    }
    void testInheritence() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);

        def settings = new BuildSettings()
        def manager = new IvyDependencyManager("test", "0.1",settings)
        try {

            settings.config.grails.test.dependency.resolution = {
                test "junit:junit:3.8.2"
            }
            // test simple exclude
            manager.parseDependencies {
                 inherits 'test'
                 runtime("opensymphony:oscache:2.4.1") {
                     excludes 'jms'
                 }
            }

            assertEquals 2, manager.listDependencies("test").size()
        }
        finally {
            BuildSettingsHolder.settings=null
        }



    }

    void testExcludes() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO);
        def manager = new IvyDependencyManager("test", "0.1")
        // test simple exclude
        manager.parseDependencies {
            runtime("opensymphony:oscache:2.4.1") {
               excludes 'jms'
            }
        }

        // test complex exclude
        manager.parseDependencies {
            runtime("opensymphony:oscache:2.4.1") {
                excludes group:'javax.jms',module:'jms'
            }
        }


    }
    void testResolve() {
        Message.setDefaultLogger new DefaultMessageLogger(Message.MSG_INFO); 
        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies TEST_DATA
        manager.resolveDependencies()        
    }

    void testListDependencies() {
        def manager = new IvyDependencyManager("test", "0.1")
        manager.parseDependencies TEST_DATA
        assertEquals 12, manager.listDependencies("build").size()
        assertEquals 21, manager.listDependencies("runtime").size()
        assertEquals 22, manager.listDependencies("test").size()
    }



    void testParseDependencyDefinition() {
        def manager = new IvyDependencyManager("test", "0.1")

        manager.parseDependencies TEST_DATA

        assertNotNull manager.dependencies
        assertFalse "should have resolved some dependencies",manager.dependencies.isEmpty()

        def orgDeps = manager.getModuleRevisionIds("org.apache.ant")

        assertEquals "should have found 3 dependencies for the given organization", 3, orgDeps.size()

        ModuleRevisionId entry = orgDeps.find { ModuleRevisionId rev -> rev.name == 'ant-junit'}

        assertEquals "org.apache.ant", entry.organisation
        assertEquals "ant-junit", entry.name
        assertEquals "1.7.1", entry.revision

        def resolvers = manager.chainResolver.resolvers

        assertEquals 4, resolvers.size()

        assertTrue "should have a file system resolver",resolvers[0] instanceof FileSystemResolver
        assertEquals "mine", resolvers[0].name
        assertTrue "should resolve to grails home",resolvers[0].artifactPatterns[0].endsWith("lib/[artifact]-[revision].[ext]")
        assertTrue "grailsHome() should be a file system resolver",resolvers[1] instanceof FileSystemResolver
        assertTrue "grailsHome() should be a file system resolver",resolvers[2] instanceof FileSystemResolver

        ModuleRevisionId junit = manager.dependencies.find {  ModuleRevisionId m -> m.organisation == 'junit'}
    }


    static final TEST_DATA = {
                repositories {
                    flatDir name: 'mine', dirs: "lib"
                    grailsHome()
                    mavenCentral()
                }
                dependencies {

                    build "org.tmatesoft.svnkit:svnkit:1.2.0",
                          "org.apache.ant:ant-junit:1.7.1",
                          "org.apache.ant:ant-nodeps:1.7.1",
                          "org.apache.ant:ant-trax:1.7.1",
                          "radeox:radeox:1.0-b2",
                          "hsqldb:hsqldb:1.8.0.10",
                          "apache-tomcat:jasper-compiler:5.5.15",
                          "jline:jline:0.9.91",
                          "javax.servlet:servlet-api:2.5",
                          "javax.servlet:jsp-api:2.1",
                          "javax.servlet:jstl:1.1.2",
                          "xalan:serializer:2.7.1"

                    test "junit:junit:3.8.2"

                    runtime "apache-taglibs:standard:1.1.2",
                            "aspectj:aspectjweaver:1.6.2",
                            "aspectj:aspectjrt:1.6.2",
                            "cglib:cglib-nodep:2.1_3",
                            "commons-beanutils:commons-beanutils:1.8.0",
                            "commons-collections:commons-collections:3.2.1",
                            "commons-dbcp:commons-dbcp:1.2.2",
                            "commons-fileupload:commons-fileupload:1.2.1",
                            "commons-io:commons-io:1.4",
                            "commons-lang:commons-lang:2.4",
                            "javax.transaction:jta:1.1",
                            "log4j:log4j:1.2.15",
                            "net.sf.ehcache:ehcache:1.6.1",
                            "opensymphony:oscache:2.4.1",
                            "opensymphony:sitemesh:2.4",
                            "org.slf4j:jcl-over-slf4j:1.5.6",
                            "org.slf4j:slf4j-api:1.5.6",
                            "oro:oro:2.0.8",
                            "xpp3:xpp3_min:1.1.3.4.O"

                    runtime "commons-validator:commons-validator:1.3.1",
                            "commons-el:commons-el:1.0"
                            [transitive:false]
                }

    }
}