package samples

import org.junit._
import Assert._
import com.github.dunnololda.simplenet._

@Test
class AppTest {

    @Test
    def testOK() {
      // arithmetic server and client
      val server = NetServer(9000, 60000)
      val client = NetClient("localhost", server.listenPort, 60000)

      client.waitConnection()
      (1 to 20).foreach { i =>
        val i1 = (math.random*100).toInt
        val i2 = (math.random*100).toInt
        val (str_op, answer) = (math.random*4).toInt match {
          case 0 => ("+", 1f*i1+i2)
          case 1 => ("-", 1f*i1-i2)
          case 2 => ("*", 1f*i1*i2)
          case 3 => if(i2 != 0) ("/", 1f*i1/i2) else ("+", 1f*i1+i2)
          case _ => ("+", 1f*i1+i2)
        }
        val question = State("a" -> i1, "b" -> i2, "op" -> str_op)
        client.send(question)
        server.waitNewEvent {
          case NewMessage(client_id, client_question) =>
            (for {
              a <- client_question.value[Float]("a")
              b <- client_question.value[Float]("b")
              op <- client_question.value[String]("op")
            } yield (a, b, op)) match {
              case Some((a, b, op)) =>
                op match {
                  case "+" => server.sendToClient(client_id, State("result" -> (a + b)))
                  case "-" => server.sendToClient(client_id, State("result" -> (a - b)))
                  case "*" => server.sendToClient(client_id, State("result" -> (a * b)))
                  case "/" => server.sendToClient(client_id, State("result" -> (a / b)))
                  case _   => server.sendToClient(client_id, State("result" -> ("unknown op: " + op)))
                }
              case None => server.sendToClient(client_id, State("result" -> "unknown data"))
            }
        }
        client.waitNewEvent {
          case NewServerMessage(server_answer) =>
            server_answer.value[Float]("result") match {
              case Some(result) => assertTrue(result == answer)
              case None => assertTrue(false)
            }
        }
      }

      assertTrue(server.clientIds.length == 1)
      assertTrue(client.isConnected)
    }
}


