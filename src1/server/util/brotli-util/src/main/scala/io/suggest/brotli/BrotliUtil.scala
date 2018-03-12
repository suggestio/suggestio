package io.suggest.brotli

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import io.suggest.util.logs.MacroLogsImpl
import org.meteogroup.jbrotli.{Brotli, BrotliStreamCompressor}
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.02.18 17:28
  * Description: Утиль, абстрагирующая нижележащую brotli-реализацию от sio-кода.
  */
object BrotliUtil extends MacroLogsImpl  {

  // Инициализировать jbrotli
  BrotliLibraryLoader.loadBrotli()

  /** Макс.размер одного chunk'а с данными.
    *
    * Max: 256 KiB.
    * Можно уточнить через new BrotliStreamCompressor( Brotli.DEFAULT_PARAMETER ).getMaxInputBufferSize()
    */
  def MAX_INPUT_CHUNK_SIZE_B: Int = 256 * 1024


  /** Сборка инстанса streamCompressor'а.
    * Т.к. компрессор нативный, то надо не забывать вычищать его из памяти через .close().
    *
    * @return Инстанс BrotliStreamCompressor.
    */
  def mkStreamCompressor(): BrotliStreamCompressor = {
    new BrotliStreamCompressor( Brotli.DEFAULT_PARAMETER )
  }


  def compress: BrotliCompress = new BrotliCompress

}


/** Сжиматель данных из потока данных. */
class BrotliCompress extends GraphStage[FlowShape[ByteString, ByteString]] {

  val (in, out): (Inlet[ByteString], Outlet[ByteString]) = {
    val prefix = getClass.getSimpleName + "."
    (
      Inlet( prefix + "In"),
      Outlet( prefix + "Out")
    )
  }

  override def shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      private var streamCompressor = BrotliUtil.mkStreamCompressor()

      /** Close streamCompressor, if not closed. */
      private def _closeStreamComp(): Unit = {
        if (streamCompressor != null) {
          streamCompressor.close()
          streamCompressor = null
        }
      }

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val input = grab(in)
          // TODO Opt Здесь вроде бы нельзя без toArray, но API компрессора ещё понимает ByteBuffer'ы.
          // Судя по сорцам, ByteBuffer'ы должны быть или direct, или RW, а ByteString возвращает heap и read-only (вроде).
          // Надо разобраться, нельзя ли обойтись без цельного копирования массива?
          val compressedBytea = streamCompressor.compressArray(input.toArray, false)
          val compressedOut = ByteString.fromArrayUnsafe( compressedBytea )
          emit(out, compressedOut)
        }

        override def onUpstreamFinish(): Unit = {
          try {
            val finalElement = ByteString.fromArrayUnsafe(
              streamCompressor.compressArray(Array.empty, true)
            )
            if (finalElement.nonEmpty)
              emit(out, finalElement)
          } finally {
            _closeStreamComp()
          }
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          _closeStreamComp()
          super.onUpstreamFailure(ex)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })

      override def postStop(): Unit = {
        super.postStop()
        // Убедиться, что streamCompressor закрыт.
        // В норме, он уже должен быть уже закрыт, но вдруг была какая-то внутренняя ошибка...
        _closeStreamComp()
      }

    }
  }

}

