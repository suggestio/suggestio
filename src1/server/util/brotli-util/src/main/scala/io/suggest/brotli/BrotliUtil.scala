package io.suggest.brotli

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import org.meteogroup.jbrotli.{Brotli, BrotliStreamCompressor}
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.02.18 17:28
  * Description: Утиль, абстрагирующая нижележащую brotli-реализацию от sio-кода.
  */
object BrotliUtil {

  // Инициализировать jbrotli
  BrotliLibraryLoader.loadBrotli()

  /** Сборка brotli-компрессора, пригодного для встраивания поток двоичных данных. */
  def streamedCompressorFlow: Flow[ByteString, ByteString, NotUsed] = {
    val streamCompressor = new BrotliStreamCompressor( Brotli.DEFAULT_PARAMETER )

    Flow[ByteString]
      // Входная байт-строка может быть любого размера, а компрессор сжирает не более maxChunkSize за раз.
      // Нужно нарезать строку на подстроки, не превышающие максимальную длину:
      .via( new Chunker( streamCompressor.getMaxInputBufferSize ) )
      // Каждую строку запихнуть в компрессор. Компрессор закрыть по завершении.
      .via( new BrotliCompressFlow(streamCompressor) )
  }

}


/** Сжиматель данных из потока данных.
  *
  * @param streamCompressor Инстанс используемого компрессора.
  */
protected class BrotliCompressFlow(streamCompressor: BrotliStreamCompressor) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in: Inlet[ByteString] = Inlet("BrotliComp.In")
  val out: Outlet[ByteString] = Outlet("BrotliComp.Out")

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val input = grab(in)
          // TODO Opt Здесь вроде бы нельзя без toArray, но API компрессора ещё понимает ByteBuffer'ы.
          // Судя по сорцам, ByteBuffer'ы должны быть или direct, или RW, а ByteString возвращает heap и read-only (вроде).
          // Надо разобраться, нельзя ли обойтись без цельного копирования массива?
          val compressedBytea = streamCompressor.compressArray(input.toArray, false)
          val compressedOut = ByteString.fromArrayUnsafe( compressedBytea )
          push(out, compressedOut)
        }

        override def onUpstreamFinish(): Unit = {
          try {
            val finalElement = ByteString.fromArrayUnsafe(
              streamCompressor.compressArray(Array.empty, true)
            )
            push(out, finalElement)
          } finally {
            streamCompressor.close()
          }
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          streamCompressor.close()
          super.onUpstreamFailure(ex)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })

    }
  }

}


// TODO Унести это в какой-нибудь пакет steams-util.
/** ByteString chunker.
  * @see [[https://doc.akka.io/docs/akka/2.5.8/stream/stream-cookbook.html?language=scala#chunking-up-a-stream-of-bytestrings-into-limited-size-bytestrings]]
  */
class Chunker(val chunkSize: Int) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val in = Inlet[ByteString]("Chunker.in")
  val out = Outlet[ByteString]("Chunker.out")

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



