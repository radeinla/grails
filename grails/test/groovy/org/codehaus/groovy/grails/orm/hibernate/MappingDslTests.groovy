/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 27, 2007
 */
package org.codehaus.groovy.grails.orm.hibernate

import javax.sql.DataSource
import org.hibernate.SessionFactory

class MappingDslTests extends AbstractGrailsHibernateTests {

    void testTableMapping() {
         DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from people")
             statement.execute()
         } finally {
             con.close()
         }
    }

    void testColumnNameMappings() {
         def p = ga.getDomainClass("PersonDSL").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()

         DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select First_Name from people")
             def result = statement.executeQuery()
             assert result.next()
             def name = result.getString('First_Name')

             assertEquals "Wilma", name

         } finally {
             con.close()
         }
    }

    void testDisabledVersion() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()


         assertNull p.version
    }

    void testEnabledVersion() {
        def p = ga.getDomainClass("PersonDSL2").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()

         assertEquals 0, p.version

        p.firstName = "Bob"
        p.save()
        session.flush()

        assertEquals 1, p.version

    }

    void testCustomHiLoIdGenerator() {
        def p = ga.getDomainClass("PersonDSL").newInstance()
         p.firstName = "Wilma"
         p.save()
         session.flush()

         assert p.id
         DataSource ds = (DataSource)applicationContext.getBean('dataSource')

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from hi_value")
             def result = statement.executeQuery()
             assert result.next()
             def value = result.getLong('next_value')

             assertEquals 1, value

         } finally {
             con.close()
         }
    }



    void testLazyinessControl() {
        def personClass = ga.getDomainClass("PersonDSL")
        def p = personClass.newInstance()
         p.firstName = "Wilma"

         p.addToChildren(firstName:"Dino")
         p.addToCousins(firstName:"Bob")
         p.save()
         session.flush()
         session.clear()

         p = personClass.clazz.get(1)

         assertTrue p.children.wasInitialized()
         assertFalse p.cousins.wasInitialized()

    }

    void testUserTypes() {
        DataSource ds = (DataSource)applicationContext.getBean('dataSource')
        def relativeClass = ga.getDomainClass("Relative")
        def r = relativeClass.newInstance()
         r.firstName = "Wilma"
         r.save()
         session.flush()

         def con
         try {
             con = ds.getConnection()
             def statement = con.prepareStatement("select * from relative")
             def result = statement.executeQuery()
             assert result.next()
             def metadata = result.getMetaData()
             assertEquals "FIRST_NAME",metadata.getColumnLabel(3)
             // hsqldb returns -1 for text type, if it wasn't mapped as text it would be 12 so this is an ok test
             assertEquals( -1, metadata.getColumnType(3) )


         } finally {
             con.close()
         }
    }

    protected void onSetUp() {
        gcl.parseClass('''        
class PersonDSL {
    Long id
    Long version
    String firstName
    Set children
    Set cousins

    static hasMany = [children:Relative, cousins:Relative]
    static mapping = {
        table 'people'
        version false
        cache usage:'read-only', include:'non-lazy'
        id generator:'hilo', params:[table:'hi_value',column:'next_value',max_lo:100]

        columns {
            firstName name:'First_Name'
            children lazy:false
        }
    }
}

class Relative {
    Long id
    Long version

    String firstName

    static mapping = {
        columns {
            firstName type:'text'
        }
    }
}

class PersonDSL2 {
    Long id
    Long version
    String firstName

    static mapping = {
        table 'people2'
        version true
        cache usage:'read-write', include:'non-lazy'
        id column:'person_id'

        columns {
            firstName name:'First_Name'
        }
    }
}
        ''')
    }




}