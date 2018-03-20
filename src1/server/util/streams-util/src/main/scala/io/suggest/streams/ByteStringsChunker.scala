package io.suggest.streams

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.02.18 11:26
  * Description: ByteString chunker -- лимитирование размера каждого ByteString в потоке.
  * @see [[https://doc.akka.io/docs/akka/2.5.8/stream/stream-cookbook.html?language=scala#chunking-up-a-stream-of-bytestrings-into-limited-size-bytestrings]]
  */
case class ByteStringsChunker(val chunkSize: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {

  val (in, out) = {
    val prefix = getClass.getSimpleName + "."
    val in1  = Inlet[ByteString]( prefix + "in")
    val out1 = Outlet[ByteString]( prefix + "out")
    (in1, out1)
  }

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    private var buffer = ByteString.empty

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (isClosed(in)) emitChunk()
        else pull(in)
      }
    })

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem = grab(in)
        buffer ++= elem
        emitChunk()
      }

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) completeStage()
        // elements left in buffer, keep accepting downstream pulls
        // and push from buffer until buffer is emitted
      }
    })

    private def emitChunk(): Unit = {
      if (buffer.isEmpty) {
        if (isClosed(in)) completeStage()
        else pull(in)
      } else {
        val (chunk, nextBuffer) = buffer.splitAt(chunkSize)
        buffer = nextBuffer
        push(out, chunk)
      }
    }

  }
}

