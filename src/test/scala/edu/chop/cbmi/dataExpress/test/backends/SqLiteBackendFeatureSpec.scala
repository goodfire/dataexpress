package edu.chop.cbmi.dataExpress.test.backends

/**
 * Created by IntelliJ IDEA.
 * User: davidsonl2
 * Date: 11/22/11
 * Time: 1:22 PM
 * To change this template use File | Settings | File Templates.
 */

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.Spec
import org.scalatest.FeatureSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.GivenWhenThen
import java.util.Properties
import edu.chop.cbmi.dataExpress.backends.SqLiteBackend
import edu.chop.cbmi.dataExpress.test.util._
import edu.chop.cbmi.dataExpress.dataModels._
import edu.chop.cbmi.dataExpress.dataModels.sql._
import edu.chop.cbmi.dataExpress.dataModels.sql.IntegerDataType


@RunWith(classOf[JUnitRunner])
class SqLiteBackendFeatureSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers {

  def fixture =
    new {
	  	val inputStream = this.getClass().getResourceAsStream("sqlite_test.properties")
        val props = new Properties()
        props.load(inputStream)
        inputStream.close()
    }

  val identifierQuote = "`"

  def dataSetupFixture =
    new {
      val tf = fixture
      val targetBackend = new SqLiteBackend(tf.props)
      targetBackend.connect
      val targetConnection = targetBackend.connection
      val targetStatement = targetConnection.createStatement()
    }

  val setup = dataSetupFixture


  def removeTestDataSetup: Boolean = {
    setup.targetStatement.execute("DROP TABLE cars_deba_a")
    setup.targetBackend.commit
    true
  }


  ignore("The user can create a table with four columns") {
    val f = fixture
    val tableName = "cars_deba_a"
    val columnFixedWidth: Boolean = false
    val columnNames: List[String] = List("carid", "carnumber", "carmake", "carmodel")
    val dataTypes = List(CharacterDataType(20, columnFixedWidth), IntegerDataType(), CharacterDataType(20, columnFixedWidth), CharacterDataType(20, columnFixedWidth))
    val verifyTableStatement: String = "SELECT COUNT(*) as 'count' FROM sqlite_master WHERE tbl_name = %s".format(tableName)
    val backend = new SqLiteBackend(f.props)
    val cascade: Boolean = true

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("the user issues a valid create table instruction for a table that does not exist")
    try {
      var tableExistResult = backend.executeQuery(verifyTableStatement)
      assert(tableExistResult.next())

      if (tableExistResult.getInt("count") != 0)
      {
        try  {
          backend.dropTable(tableName,cascade)
        }
        catch {
        case e:java.sql.SQLException =>
            println(e.getMessage + "\n")
            fail( "backend.dropTable(" + "\"" + tableName + "," + "\"" + ")produced java.sql.SQLException" +
                  "when attempting to drop existing table" )
        }

      tableExistResult.close()

      tableExistResult                      =     backend.executeQuery(verifyTableStatement)
      assert(tableExistResult.next())

          if (tableExistResult.getInt("count") != 0)
          {

            fail( "Unable to drop existing table " + tableName )
          }


      }
    }
    catch {
    case e:java.sql.SQLException =>
            println(e.getMessage + "\n")
            fail("backend.executeQuery(" + verifyTableStatement + ")produced java.sql.SQLException" )
    }

    /* Table should be dropped now if it existed) */

    try
      backend.createTable(tableName,columnNames,dataTypes)
    catch {
    case e:java.sql.SQLException =>
            println(e.getMessage + "\n")
            fail("backend.createTable(" + "\"" + tableName + "\"" + ")produced java.sql.SQLException" )
    }


    then("the table should exist")
    val tableExistResult = backend.executeQuery(verifyTableStatement)
    assert(tableExistResult.next())
    tableExistResult.getInt("count") should equal(1)
    backend.close()
  }



  ignore("The user can truncate a table and commit") {
    val f = fixture
    val tableName: String = "cars_deba_a"
    val countStatement: String = "select count(1) as 'count' from " + tableName
    val backend = new SqLiteBackend(f.props)

     given("an active connection and a populated table")
     assert(backend.connect().isInstanceOf[java.sql.Connection] )
     backend.connection.setAutoCommit(false)


     when("the user issues truncate and then commit instructions for that table")
     try
       backend.truncateTable(tableName)
     catch {
     case e:java.sql.SQLException =>
             fail("backend.truncateTable(" + "\"" + tableName  + "\"" + ")produced java.sql.SQLException" )
     }

     try
       backend.commit()
     catch {
     case e:java.sql.SQLException =>
             fail("backend.commit()produced java.sql.SQLException" )
     }

     then("the table should be truncated")
     val countResult                                 =     backend.executeQuery(countStatement)
     assert(countResult.next())
     countResult.getInt("count") should equal (0)

     backend.close()

   }



  //TODO: This test needs to be re-written with an auto-incrementing sequence in the table to fully test insert returning keys
  ignore("The inserted row can be committed") {
    val f             = fixture

    val backend       = new SqLiteBackend(f.props)

    val tableName     = "cars_deba_a"

    val columnNames:List[String]      = List("carid","carnumber","carmake","carmodel")

    val valuesHolders:List[String]    = for (i <- (0 to (columnNames.length - 1)).toList) yield "?"

    val sqlStatement                  = "insert into " + tableName  +
                                      "("                                                                           +
                                      columnNames.map(i => i + ",").mkString.dropRight(1)                           +
                                      ")"                                                                           +
                                      " values (" + valuesHolders.map(i => i + ",").mkString.dropRight(1)  + ")"


    val carId:String              = "K0000001"

    val carNumber:Int                       = 1234567890

    val carMake                     = "MiniCoopeRa"

    val carModel                      = "One"

    val valuesList                    = List(carId,carNumber,carMake,carModel)

    val bindVars:DataRow[Any]         =
      DataRow((columnNames(0),valuesList(0)),(columnNames(1),valuesList(1)),(columnNames(2),valuesList(2)),(columnNames(3),valuesList(3)))

    var isDataRow:Boolean             = false

    var insertedRow:DataRow[Any]           = DataRow.empty

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("an insert query is executed and committed")
    try
      backend.execute(sqlStatement,bindVars)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.execute(" + "\"" + sqlStatement + "\"" + ")produced java.sql.SQLException" )
    }


    try
      backend.commit()
    catch {
    case e:java.sql.SQLException =>
            fail("backend.commit()produced java.sql.SQLException" )
    }

    then("the inserted row should be in the database")
    
    val rs = backend.executeQuery("select count(*) from %s where %s = ?".format(tableName, columnNames(0)), List(Option(carId)))
    rs.next
    rs.getInt(1) should equal(1)
    
    backend.close()
  }



  ignore("The user can obtain a record from executing a select query") {
    //Prerequisites:  ignore 1:  Passed

    val f = fixture
    val backend = new SqLiteBackend(f.props)
    val tableName = "cars_deba_a"
    val sqlStatement = "select * from " + tableName + " where carid  = ?"
    val valuesList: List[String] = List("K0000001")
    val columnNames: List[String] = List("carid")
    val bindVars: DataRow[String] = DataRow((columnNames(0), valuesList(0)))
    var hasResults: Boolean = false

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection])
    backend.connection.setAutoCommit(false)

    when("a query that should generate results is executed")
    val resultSet = backend.executeQuery(sqlStatement, bindVars)

    then("one or more results should be returned")
    hasResults = resultSet.next()
    hasResults should be(true)

    backend.close()
  }





  ignore("The user can determine whether a select query has returned a record") {

    //Prerequisites:  ignore 1:  Passed

    val f             =   fixture
    val backend       =   new SqLiteBackend(f.props)
    val tableName     =   "cars_deba_a"
    val sqlStatement  =   "select * from " + tableName + " where carid  = ?"
    val columnNames                               = List("carid")
    val valuesList                                = List("K0000001")
    val bindVars:DataRow[String]                  = DataRow((columnNames(0),valuesList(0)))

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("a select query that has a non empty result is executed")
    //resultSetReturned                             = backend.execute(sqlStatement,bindVars)
    //resultSetReturned seems to only be true only if it is an update count or if execute does not return anything at all
    try {
      val results = backend.executeQuery(sqlStatement, bindVars)
      then("the query should have returned a non empty result set")
      val nonEmptyResultSet: Boolean = results.next()
      nonEmptyResultSet should be(true)
    }
    catch {
    case  e:java.sql.SQLException =>
            fail("backend.executeQuery(" + sqlStatement + ")produced java.sql.SQLException" )
    }


    backend.close()


  }







  ignore("The user can commit an open transaction") {

    val f                             = fixture
    var backend                       = new SqLiteBackend(f.props)
    val tableName                     = "cars_deba_a"
    val columnNames:List[String]      = List("carid","carnumber","carmake","carmodel")
    val valuesHolders:List[String]    = for (i <- (0 to (columnNames.length - 1)).toList ) yield "?"
    var sqlStatement                  = "insert into " + tableName                     +
                                      "("                                                               +
                                      columnNames.map(i => i + ",").mkString.dropRight(1)               +
                                      ")"                                                               +
                                      " values (" + valuesHolders.map(i => i + ",").mkString.dropRight(1)  + ")"
    val carId: String = "K0000002"
    val carNumber: Int = 1234567899
    val carMake = "MiniCoopeRa"
    val carModel = "Two"
    val valuesList = List(carId, carNumber, carMake, carModel)
    val bindVars: DataRow[Any] = DataRow(("carid", carId), ("carnumber", carNumber), ("carmake", carMake), ("carmodel", carModel))
    var committed: Boolean = false
    var connectionClosed: Boolean = false
    var dataPersistent: Boolean = false

    given("an active connection with an open transaction ")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)
    backend.startTransaction()
    val insertedRow = backend.executeReturningKeys(sqlStatement, bindVars)

    when("the user issues a commit instruction")
    try
      backend.commit()
    catch {
    case e:java.sql.SQLException =>
            fail("backend.commit()produced java.sql.SQLException" )
    }

    then("the data should be persisted")
    backend.close()
    connectionClosed                    = backend.connection.isClosed
    connectionClosed  should be (true)
    sqlStatement                        = """select * from %s  
      										  where carid = ?
                                                and carnumber = ?
                                                and carmake = ?
                                                and carmodel = ?""".format(tableName)

    val newFixture = fixture
    backend = new SqLiteBackend(newFixture.props)
    assert(backend.connect().isInstanceOf[java.sql.Connection])
    dataPersistent = backend.execute(sqlStatement, bindVars)
    dataPersistent should be(true)
    backend.close()
  }


  ignore("The user can truncate a populated table") {
    val f = fixture
    val tableName: String = "cars_deba_a"
    val countStatement: String = "select count(1) as 'count' from " + tableName
    val backend = new SqLiteBackend(f.props)

    given("an active connection and a populated table")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)
    var countResult = backend.executeQuery(countStatement)
    countResult.next() should  be (true)
    countResult.getInt("count") should be > (0)

    when("the user issues a truncate table instruction for that table")
    try
      backend.truncateTable(tableName)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.truncateTable(" + "\"" + tableName + "\"" + ")produced java.sql.SQLException" )
    }

    then("the table should be truncated")
    countResult = backend.executeQuery(countStatement)
    assert(countResult.next())
    countResult.getInt("count") should equal(0)

    backend.close()

  }


  ignore("The user can roll back an open transaction") {
    val f = fixture
    val backend = new SqLiteBackend(f.props)
    val tableName = "cars_deba_a"
    val columnNames: List[String] = List("carid", "carnumber", "carmake", "carmodel")
    val valuesHolders: List[String] = for (i <- (0 to (columnNames.length - 1)).toList) yield "?"
    var sqlStatement                  = "insert into " + tableName                     +
                                      "("                                                               +
                                      columnNames.map(i => i + ",").mkString.dropRight(1)               +
                                      ")"                                                               +
                                      " values (" + valuesHolders.map(i => i + ",").mkString.dropRight(1)  + ")"

    val carId: String = "K0000050"
    val carNumber: Int = 1234567777
    val carMake = "MiniCoopeRa"
    val carModel = "Fifty"
    val valuesList = List(carId, carNumber, carMake, carModel)
    val bindVars: DataRow[Any] = DataRow(("carid", carId), ("carnumber", carNumber), ("carmake", carMake), ("carmodel", carModel))
    var connectionClosed: Boolean = false
    var dataPersistent: Boolean = false

    given("an active connection with an open transaction ")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)
    backend.startTransaction()
    backend.execute(sqlStatement,bindVars)
    //can't depend on the row coming back
    //assert(insertedRow.isInstanceOf[DataRow[Any]])

    when("the user issues a rollback instruction")
    try
      backend.rollback()
    catch {
    case e:java.sql.SQLException =>
            fail("backend.rollback(" + "\"" + tableName +  "\"" + ")produced java.sql.SQLException" )
    }

    then("the data should not be persisted")
    backend.close()
    connectionClosed = backend.connection.isClosed
    connectionClosed should be(true)
    sqlStatement                      = """select * from %s  
      										  where carid = ?
                                                and carnumber = ?
                                                and carmake = ?
                                                and carmodel = ?""".format(tableName)
    val newFixture = fixture
    val newBackend = new SqLiteBackend(newFixture.props)
    assert(newBackend.connect().isInstanceOf[java.sql.Connection])
    val persistentDataCount = newBackend.executeQuery(sqlStatement, bindVars)
    assert(persistentDataCount.next())
    persistentDataCount.getInt("count") should equal(0)
    newBackend.close()

  }


  ignore("The user can open a transaction, insert a row, and end the transaction") {

    val f                             = fixture
    val backend                       = new SqLiteBackend(f.props)

    val tableName                     = "cars_deba_a"

    val columnNames:List[String]      = List("carid","carnumber","carmake","carmodel")

    val valuesHolders:List[String]    = for (i <- (0 to (columnNames.length - 1)).toList ) yield "?"

    var sqlStatement                  = "insert into " + tableName                    +
                                      "("                                                               +
                                      columnNames.map(i => i + ",").mkString.dropRight(1)               +
                                      ")"                                                               +
                                      " values (" + valuesHolders.map(i => i + ",").mkString.dropRight(1)  + ")"

    val carId:String              = "K0000055"

    val carNumber:Int                       = 1234567755

    val carMake                     = "MiniCoopeRa"

    val carModel                      = "FiftyFive"

    val valuesList                    = List(carId,carNumber,carMake,carModel)

    val bindVars:DataRow[Any]         = DataRow((columnNames(0),valuesList(0)),(columnNames(1),valuesList(1)),(columnNames(2),valuesList(2)),(columnNames(3),valuesList(3)))

    var connectionClosed:Boolean      = false



    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("the user issues a start transaction instruction")
    try
            backend.startTransaction()          //This should be replaced with backend.startTransaction when available
    catch {
    case    e:java.sql.SQLException =>
            println(e.getMessage)
            fail("backend.startTransaction() produced java.sql.SQLException" )
    }

    and("the user inserts a row")
    try   {
            val insertedRow                   = backend.executeReturningKeys(sqlStatement,bindVars)
            assert(insertedRow.isInstanceOf[DataRow[Any]])
    }
    catch {
    case    e:java.sql.SQLException =>
            println(e.getMessage)
            fail("backend.executeReturningKeys(" + sqlStatement + ") produced java.sql.SQLException" )
    }

    and("the user ends the transaction")
    try
            backend.endTransaction()
    catch {
    case    e:java.sql.SQLException =>
            fail("backend.endTransaction() produced java.sql.SQLException" )
    }

    then("the data should be persisted")
    backend.close()
    connectionClosed                  = backend.connection.isClosed
    connectionClosed  should be (true)
    sqlStatement                      = "select count(1) as 'count' from " + tableName    +
                                        " where "                                                         +
                                        " carid  = ? "            +      " and "                      +
                                        " carnumber  = ? "                  +      " and "                      +
                                        " carmake  = ? "            +      " and "                      +
                                        " carmodel  = ? "

    val newFixture                            = fixture
    val newBackend                            = new SqLiteBackend(newFixture.props)
    assert(newBackend.connect().isInstanceOf[java.sql.Connection] )
    val persistentDataCount                   = newBackend.executeQuery(sqlStatement,bindVars)
    assert(persistentDataCount.next() )
    persistentDataCount.getInt("count") should equal (1)
    newBackend.close()

  }


  ignore("The user can create a table with 32 columns") {
    val f = fixture

    val tableName                             =     "cars_deba_b"

    val columnFixedWidth:Boolean              =     false

    val columnNames:List[String]              =     List("carid","carnumber","carmake","carmodel")

    val dataTypes                             =     List( CharacterDataType(20,columnFixedWidth),IntegerDataType(),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),
                                                          CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth)
                                                    )

    val verifyTableStatement: String = "SELECT COUNT(*) as 'count' FROM sqlite_master WHERE tbl_name = %s".format(tableName)

    val backend                               =     new SqLiteBackend(f.props)

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("the user issues a valid create table instruction")
    try
      backend.createTable(tableName,columnNames,dataTypes)
    catch {
      case e:java.sql.SQLException => {
        println(e.getCause + "\n" + e.getMessage + "\n" + e.getSQLState + "\n"  )
        //fail("backend.createTable(" + "\"" + tableName + "," + dbSchema.get + "\"" + ")produced java.sql.SQLException" )
      }

    }


    then("the table should exist")
    val tableExistResult                                      =     backend.executeQuery(verifyTableStatement)
    assert(tableExistResult.next())
    tableExistResult.getInt("count") should equal (1)


    backend.close()
  }






  ignore("The user can insert a row without constructing an insert statement") {
    val f = fixture

    val tableName:String                      =     "cars_deba_a"

    val columnNames:List[String]              =     List("carid","carnumber","carmake","carmodel")

    val carId:String                      =     "K0000003"

    val carNumber:Int                               =     1234567888

    val carMake:String                      =     "MiniCoopeRa"

    val carModel:String                       =     "Three"

    val valuesList:List[Any]                  =     List(carId,carNumber,carMake,carModel)

    val row:DataRow[Any]                      =     DataRow(("carid",carId),("carnumber",carNumber),("carmake",carMake),("carmodel",carModel))

    val backend                               =     new SqLiteBackend(f.props)

    val verifyRecordStatement:String          =     "select count(1) as count from " + tableName+ " where "    +
                                                    "carid = " + "'" + row.carid.get  + "'"


    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("the user issues a valid insert command for an existing table and a unique record")
    var recordCountResult                         =     backend.executeQuery(verifyRecordStatement)
    assert(recordCountResult.next())
    var recordCount                               =     recordCountResult.getInt("count")
    recordCount should be (0)
     backend.insertRow(tableName,row)


    and("the row should be inserted")
    recordCountResult                             =     backend.executeQuery(verifyRecordStatement)
    assert(recordCountResult.next())
    recordCount                                   =     recordCountResult.getInt("count")
    recordCount  should be (1)

    backend.commit()

    backend.close()

  }







  ignore("The user can insert a batch of rows and commit without having to construct the insert statements") {
    val f = fixture

    val tableName:String                      =     "cars_deba_a"

    val columnNames:List[String]              =     List("carid","carnumber","carmake","carmodel")

    val rowOne:List[Any]                      =     List("K0000201",1234901,"MiniCoopeRb","One")
    val rowTwo:List[Any]                      =     List("K0000202",1234902,"MiniCoopeRb","Two")
    val rowThree:List[Any]                    =     List("K0000203",1234903,"MiniCoopeRb","Three")
    val rowFour:List[Any]                     =     List("K0000204",1234904,"MiniCoopeRb","Four")
    val rowFive:List[Any]                     =     List("K0000205",1234905,"MiniCoopeRb","Five")
    val rowSix:List[Any]                      =     List("K0000206",1234906,"MiniCoopeRb","Six")
    val rowSeven:List[Any]                    =     List("K0000207",1234907,"MiniCoopeRb","Seven")
    val rowEight:List[Any]                    =     List("K0000208",1234908,"MiniCoopeRb","Eight")
    val rowNine:List[Any]                     =     List("K0000209",1234909,"MiniCoopeRb","Nine")
    val rowTen:List[Any]                      =     List("K0000210",1234910,"MiniCoopeRb","Ten")

    val rows                                  =     List(rowOne,rowTwo,rowThree,rowFour,rowFive,rowSix,rowSeven,rowEight,rowNine,rowTen)

    val table:DataTable[Any]                  =     DataTable(columnNames, rowOne,rowTwo,rowThree,rowFour,rowFive,rowSix,rowSeven,rowEight,rowNine,rowTen)

    val backend                               =     new SqLiteBackend(f.props)

    var successfulStatementCount:Int          =     0

    val verifyRowsStatement:String            =     "select count(1) as count from " + tableName+ " where "     +
                                                    "carid in "                                                                           +
                                                    " ("                                                                                      +
                                                    {for (i <- 0 to (rows.length - 1)) yield "'" + rows(i).toList.head.toString + "'"}.toString().dropRight(1).drop(7) +
                                                    ")"

    given("an active connection and an empty table")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)
    try
      backend.truncateTable(tableName)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.truncateTable(" + "\"" + tableName + "\"" + ")produced java.sql.SQLException" )
    }

    when("the user issues a batch insert command (with commit) to insert multiple rows into the table ")
    successfulStatementCount                      =     backend.batchInsert(tableName, table)
    try
      backend.commit()
    catch {
    case e:java.sql.SQLException =>
            fail("backend.commit() produced java.sql.SQLException" )
    }

    then("the batch insert command should be successful")
    successfulStatementCount  should equal  (rows.length)


    and("the rows should be inserted")
    val recordCountResult                         =     backend.executeQuery(verifyRowsStatement)
    assert(recordCountResult.next())
    val recordCount                               =     recordCountResult.getInt("count")
    recordCount  should be (10)

    backend.close()



  }




  ignore("The user can drop a table") {
    val f = fixture

    val tableName:String                      =     "cars_deba_c"

    val columnFixedWidth:Boolean              =     false

    val columnNames:List[String]              =     List("carid","carnumber","carmake","carmodel")

    val dataTypes                             =     List(CharacterDataType(20,columnFixedWidth),IntegerDataType(),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth))

    val verifyTableStatement: String = "SELECT COUNT(*) as 'count' FROM sqlite_master WHERE tbl_name = %s".format(tableName)
    val backend                               =     new SqLiteBackend(f.props)



    given("an active connection and an existing table")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)
    try
      backend.createTable(tableName,columnNames,dataTypes)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.createTable(" + "\"" + tableName + "," + "\"" + ")produced java.sql.SQLException" )
    }
    val tableVerifiedResult                    =     backend.executeQuery(verifyTableStatement)
    assert(tableVerifiedResult.next())
    tableVerifiedResult.getInt("count") should be (1)

    when("the user issues a drop table command for that table")
    try
      backend.dropTable(tableName)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.dropTable(" + "\"" + tableName + "," + "\"" + ")produced java.sql.SQLException" )
    }


    then("the table should be dropped")
    val tableExistResult                    =     backend.executeQuery(verifyTableStatement)
    assert(tableExistResult.next())
    tableExistResult.getInt("count") should be (0)

    backend.close()

  }






  ignore("The user can drop a table with cascade") {

    /*  Drop table cascade is only for portabilty and has no effects                            */
    /*  http://stackoverflow.com/questions/3476765/mysql-drop-all-tables-ignoring-foreign-keys  */

    val f = fixture

    val tableName:String                      =     "cars_deba_c"

    val viewName:String                       =     "cars_deba_c_v"

    val columnFixedWidth:Boolean              =     false

    val columnNames:List[String]              =     List("carid","carnumber","carmake","carmodel")

    val dataTypes                             =     List(CharacterDataType(20,columnFixedWidth),IntegerDataType(),CharacterDataType(20,columnFixedWidth),CharacterDataType(20,columnFixedWidth))

    val verifyTableStatement: String = "SELECT COUNT(*) as 'count' FROM sqlite_master WHERE tbl_name = %s".format(tableName)


    val backend                               =     new SqLiteBackend(f.props)



    val cascade                               =     true

    given("an active connection, an existing table, and a view on the existing table")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    try
      backend.createTable(tableName,columnNames,dataTypes)
    catch {
    case  e:java.sql.SQLException =>
          println(e.getMessage)
          fail("backend.createTable(" + "\"" + tableName + "," + "\"" + ")produced java.sql.SQLException" )
    }

    var tableVerifiedResult                   =     backend.executeQuery(verifyTableStatement)
    assert(tableVerifiedResult.next())
    tableVerifiedResult.getInt("count") should  equal  (1)

    when("the user issues a drop table command with cascade for that table")
    try
      backend.dropTable(tableName, cascade)
    catch {
    case e:java.sql.SQLException =>
            println(e.getMessage)
            fail("backend.dropTable(" + "\"" + tableName + "," + "\"" + ")produced java.sql.SQLException" )
    }


    then("the table  be dropped")
    val tableExistResult                    =     backend.executeQuery(verifyTableStatement)
    assert(tableExistResult.next())
    tableExistResult.getInt("count")  should be (0)

    backend.close()

  }





  ignore("The user can iterate over the results of a select query") {
    //Prerequisites:  Need Multiple Row in table cars_deba_a

    val f                     = fixture

    val backend               = new SqLiteBackend(f.props)

    val tableName             = "cars_deba_a"

    val sqlStatement          = "select * from " + tableName

    val bindVars:DataRow[String]                  = DataRow.empty

    var resultsCount:Int                          = 0

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("a query that should generate multiple results is executed")
    val resultSet = backend.executeQuery(sqlStatement,bindVars)

    then("the user should be able to iterate over the results")
    while (resultSet.next()) { resultsCount+=1 }


    and("multiple results should be returned")
    resultsCount should be > (1)

    backend.close()
  }




  ignore("The user can update a record in a table using a valid update statement") {
    //Prerequisites:  Need Multiple Row in table cars_deba_a

    val f                             = fixture

    val backend                       = new SqLiteBackend(f.props)

    val tableName                     = "cars_deba_a"

    var columnNames:List[String]      = List("carid","carnumber","carmake","carmodel")


    val carId:String              = "K0000210"

    val carNumber:Int                       = 1234567899

    val carMake                     = "MiniCoopeRa"

    val carModel                      = "FourteenMillion"

    var sqlStatement                  = "update " + tableName                           +
                                        " set "                                                           +
                                        columnNames.map(i => i + " = ?,").mkString.dropRight(1)           +
                                        " where carid = " + "'" + carId  + "'"

    var valuesList:List[Any]          = List(carId,carNumber,carMake,carModel,carId)

    var bindVars:DataRow[Any]         = DataRow(("carid",carId),("carnumber",carNumber),("carmake",carMake),("carmodel",carModel))

    var resultsCount:Int              = 0

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("an update query is executed")
    try
      backend.execute(sqlStatement,bindVars)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.execute(" + "\"" + sqlStatement + "\"" + ")produced java.sql.SQLException" )
    }


    then("the record(s) should be updated")
    columnNames                                   = List("carmodel")
    valuesList                                    = List(carModel)
    bindVars                                      = DataRow(("carmodel",carModel))
    sqlStatement                                  = "select count(1) as count from " + tableName + " where "     +
                                                    "carmodel = ?"

    val recordCountResult                         = backend.executeQuery(sqlStatement,bindVars)
    assert(recordCountResult.next())
    resultsCount                                  = recordCountResult.getInt("count")
    resultsCount  should be (1)


    backend.close()
  }


  ignore("The user can update a multiple records in a table using a valid update statement") {
    //Prerequisites:  Need Multiple Row in table cars_deba_a

    val f                             = fixture

    val backend                       = new SqLiteBackend(f.props)

    val tableName                     = "cars_deba_a"

    var columnNames:List[String]      = List("carmodel")

    var sqlStatement                  = "update " + tableName                           +
                                        " set "                                                           +
                                        columnNames.map(i => i + " = ?,").mkString.dropRight(1)


    val carModel                                  = "SeventeenMillion"

    val valuesList:List[Any]                      = List(carModel)

    val bindVars:DataRow[Any]                     = DataRow((columnNames(0),carModel))

    var resultsCount:Int                          = 0

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("an update query for multiple records is executed")
    try
      backend.execute(sqlStatement,bindVars)
    catch {
    case e:java.sql.SQLException =>
            fail("backend.execute(" + "\"" + sqlStatement + "\"" + ")produced java.sql.SQLException" )
    }

    then("multiple record(s) should be updated")
    sqlStatement                                  = "select count(1) as count from " + tableName+ " where "     +
                                                    "carmodel = ?"

    val recordCountResult                         = backend.executeQuery(sqlStatement,bindVars)
    assert(recordCountResult.next())
    resultsCount                                  = recordCountResult.getInt("count")
    resultsCount  should be > (1)
    //It would be better to compare the number of rows updated to the count  i.e.:
    //http://www.coderanch.com/t/426288/JDBC/java/Row-count-update-statement
    //Statement st = connection.createStatement("update t_number set number = 2 where name='abcd");
    //int rowCount = st.executeUpdate();
    //However, this executeUpdate is not yet available on the backend


    backend.close()
  }





  ignore("The user can update a multiple records in a table without constructing update statement") {
    //Prerequisites:  Need Multiple Row in table cars_deba_a   with carmake = 'MiniCoopeRb'

    val f                                         = fixture

    val backend                                   = new SqLiteBackend(f.props)

    val tableName                                 = "cars_deba_a"

    var columnNames:List[String]                  = List("carnumber","carmake","carmodel")

    val carNumber:Int                                   = 192837465

    val carMake                                 = "MiniCoopeRaStyle004"

    val carModel                                  = "SeventeenMillion"

    val valuesList:List[Any]                      = List(carNumber,carMake,carModel)

    val filter:List[(String, Any)]                = List(("carmake","MiniCoopeRb"))

    val updatesBindVars:DataRow[Any]              =
      DataRow((columnNames(0),valuesList(0)),(columnNames(1),valuesList(1)),(columnNames(2),valuesList(2)))

    var resultsCount:Int                          = 0

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("an update row instruction for multiple records is executed")
    try
      backend.updateRow(tableName,updatesBindVars,filter)
    catch {
    case e:java.sql.SQLException =>
            println(e.getMessage())
            fail("backend.updateRow()produced java.sql.SQLException" )
    }

    then("multiple record(s) should be updated")
    val sqlStatement                              = "select count(1) as count from " + tableName+ " where "     +
                                                    "carmake = ?"


    val bindVars:DataRow[Any]                     =
      DataRow((columnNames(1),valuesList(1)))

    val recordCountResult                         = backend.executeQuery(sqlStatement,bindVars)
    assert(recordCountResult.next())
    resultsCount                                  = recordCountResult.getInt("count")
    resultsCount  should be > (1)
    //It would be better to compare the number of rows updated to the count  i.e.:
    //http://www.coderanch.com/t/426288/JDBC/java/Row-count-update-statement
    //Statement st = connection.createStatement("update t_number set number = 2 where name='abcd");
    //int rowCount = st.executeUpdate();
    //However, this executeUpdate is not yet available on the backend


    backend.close()
  }





  ignore("The user can insert a multiple rows using a loop without constructing an insert statement") {
    //Prerequisites:  None of theses record should exist
    val f = fixture

    val tableName:String                      =     "cars_deba_a"

    val columnNames:List[String]              =     List("carid","carnumber","carmake","carmodel")

    val carIds:List[String]                   =     List("K0000500","K0000501","K0000502","K0000503","K0000504")

    val carNumbers:List[Int]                  =     List(1234561000,1234561001,1234561002,1234561003,1234561004)

    val carMakes:List[String]                 =     List("MiniCoopeRd","MiniCoopeRd","MiniCoopeRd","MiniCoopeRd","MiniCoopeRd")

    val carModels:List[String]                =     List("Zero","One","Ten","Ten","Ten")

    val backend                               =     new SqLiteBackend(f.props)

    val verifyRecordsStatement:String         =     "select count(1) as count from " + tableName+ " where "          +
                                                    "carid in "                                                   +
                                                    "("                                                               +
                                                    carIds.map(i => "'" + i + "'" + ",").mkString.dropRight(1)             +
                                                    ")"

    var recordCount:Int                       =     0

    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("the user issues valid insert row commands in a loop for an existing table")

    for (i <- 0 to (carIds.length -1))
    {

      val row:DataRow[Any]                     =     DataRow(("carid",carIds(i)),("carnumber",carNumbers(i)),("carmake",carMakes(i)),("carmodel",carModels(i)))
      assert(backend.insertReturningKeys(tableName,row).isInstanceOf[DataRow[Any]]  )


    }


    then("the rows should be inserted")
    val recordCountResult                       =     backend.executeQuery(verifyRecordsStatement)
    assert(recordCountResult.next())
    recordCount                                 =     recordCountResult.getInt("count")
    recordCount  should equal (carIds.length)

    backend.close()

  }





  ignore("The user can delete multiple records in a table using a valid delete statement") {
    //Prerequisites:  Need Multiple Row in table cars_deba_a

    val f                             = fixture

    val backend                       = new SqLiteBackend(f.props)

    val tableName                     = "cars_deba_a"

    val columnNames:List[String]      = List("carmodel")

    var sqlStatement                  = if    (columnNames.length > 1)  {
                                          "delete from " + tableName                                +
                                          " where "                                                                   +
                                          columnNames.map(i => i + " = ?").mkString(" and ")
                                        }
                                        else{
                                          "delete from " + tableName                                +
                                          " where "                                                                   +
                                          columnNames.map(i => i + " = ?").mkString
                                        }

    val carModel                                  = "Ten"

    val valuesList:List[Any]                      = List(carModel)

    val bindVars:DataRow[Any]                     = DataRow((columnNames(0),valuesList(0)))



    given("an active connection")
    assert(backend.connect().isInstanceOf[java.sql.Connection] )
    backend.connection.setAutoCommit(false)

    when("a delete query for multiple records is executed")
    try
      backend.execute(sqlStatement,bindVars)
    catch {
    case e:java.sql.SQLException =>
            println(e.getMessage)
            fail("backend.execute(" + "\"" + sqlStatement + "\"" + ")produced java.sql.SQLException" )
    }

    then ("those records should be deleted")
    sqlStatement                      =   "select count(1) as count from "   + tableName            +
                                          " where carmodel = ?"

    try   {
          val countResult = backend.executeQuery(sqlStatement,bindVars)
          assert(countResult.next())
          countResult.getInt("count") should equal  (0)
    }
    catch {
    case  e:java.sql.SQLException =>
            println(e.getMessage + "\n\n")
            fail("backend.executeQuery(" + "\"" + sqlStatement + "\"" + ")produced java.sql.SQLException" )






    }


  }




  ignore("The user can drop all tables that begin with a certain string") {

      /*http://stackoverflow.com/questions/3476765/mysql-drop-all-tables-ignoring-foreign-keys*/

      val sf = fixture

      val tf = fixture

      val searchString:String                   =         "cars_deba"
      val objectStatement: String = """SELECT tbl_name from sqlite_master 
                                        WHERE tabl_name like '""" + searchString + "%'" 

      val verifyObjectCountStatement:String     = """SELECT count(tbl_name) from sqlite_master 
                                                      WHERE tabl_name like '""" + searchString + "%'" 

      val sourceBackend                         =         new SqLiteBackend(sf.props)

      val targetBackend                         =         new SqLiteBackend(tf.props)

      val cascade:Boolean                       =         true

      given("an active connection")
      assert(sourceBackend.connect().isInstanceOf[java.sql.Connection] )
      assert(targetBackend.connect().isInstanceOf[java.sql.Connection] )
      targetBackend.connection.setAutoCommit(false)

      when("the user issues drop table commands for views and tables that begin with a certain string")
     // try {

      val  objectResult                         =         sourceBackend.executeQuery(objectStatement)

          try {

            while(objectResult.next())  {

              targetBackend.dropTable(objectResult.getString("table_name"), cascade)

            }
          }
          catch {
          case e:java.sql.SQLException =>
              println(e.getMessage)
              fail("targetBackend.dropTable(" + "\"" + objectResult.getString("table_name") + "\"" + ")produced java.sql.SQLException" )
          }
     // }
      //catch {
      /*case e:java.sql.SQLException =>
              println(e.getMessage)
              fail("backend.executeQuery(" + objectStatement + ")produced java.sql.SQLException" )
      }*/


      then("then those tables and views should be dropped")
      val objectExistResult                      =     targetBackend.executeQuery(verifyObjectCountStatement)
      assert(objectExistResult.next())
      objectExistResult.getInt("count")  should be (0)


      sourceBackend.close()
      targetBackend.close()

  }




  ignore("Remove Test Data Setup")  {
    /**** Remove Test Data    ****/
    removeTestDataSetup
    /****                     ****/

  }

  ignore("Close Test SetUp Connections")  {

    setup.targetBackend.close

  }





}
