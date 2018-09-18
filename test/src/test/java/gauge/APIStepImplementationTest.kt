// TODO: move to test folder once APIStepImplementation has been added to deon-dsl

package gauge

import com.deondigital.cucumber.regex
import com.deondigital.cucumber.replaceVars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class APIStepImplementationTest{

    @Test
    fun `replaceVars - one occurence`(){
        val testString = "bla$(x)blub"
        val replaced = replaceVars(testString, mapOf(Pair("x", "FOO")))
        assertEquals("blaFOOblub", replaced)
    }

    @Test
    fun `replaceVars - same multiple occurence`(){
        val testString = "bla$(x)blu$(x)b"
        val replaced = replaceVars(testString, mapOf(Pair("x", "FOO")))
        assertEquals("blaFOObluFOOb", replaced)
    }

    @Test
    fun `replaceVars - different multiple occurence`(){
        val testString = "b$(y)la$(x)blu$(x)b"
        val replaced = replaceVars(testString, mapOf(Pair("x", "FOO"), Pair("y", "BAR")))
        assertEquals("bBARlaFOObluFOOb", replaced)
    }

    @Test
    fun `replace actual test`(){
        val testString = "EnrolementRequest{timestamp=#2018-07-25T17:01:00Z#,agent=$(arg1),requester=Participant{id=$(arg2),status=Inactive},requesteeId=$(arg1)}"
        val replaced = replaceVars(testString, mapOf(Pair("arg1", "FOO"), Pair("arg2", "BAR")))
        assertEquals("EnrolementRequest{timestamp=#2018-07-25T17:01:00Z#,agent=FOO,requester=Participant{id=BAR,status=Inactive},requesteeId=FOO}",replaced)
    }

    @Test
    fun `match - one occurence`(){
        val testString = "bla$(x)blub"
//        assertTrue(testString.matches(regex))
        assertTrue(testString.matches(Regex(".*"+ regex.pattern+".*")))
    }

    @Test
    fun `match - same multiple occurence`(){
        val testString = "bla$(x)blu$(x)b"
        val replaced = replaceVars(testString, mapOf(Pair("x", "FOO")))
        assertEquals("blaFOObluFOOb", replaced)
    }

    @Test
    fun `match - different multiple occurence`(){
        val testString = "b$(y)la$(x)blu$(x)b"
        val replaced = replaceVars(testString, mapOf(Pair("x", "FOO"), Pair("y", "BAR")))
        assertEquals("bBARlaFOObluFOOb", replaced)
    }

    @Test
    fun `match - actual test`(){
        val testString = "EnrolementRequest{timestamp=#2018-07-25T17:01:00Z#,agent=$(arg1),requester=Participant{id=$(arg2),status=Inactive},requesteeId=$(arg1)}"
        val replaced = replaceVars(testString, mapOf(Pair("arg1", "FOO"), Pair("arg2", "BAR")))
        assertEquals("EnrolementRequest{timestamp=#2018-07-25T17:01:00Z#,agent=FOO,requester=Participant{id=BAR,status=Inactive},requesteeId=FOO}",replaced)
    }

}