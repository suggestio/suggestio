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
object BrotliUtil {

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
  protected[brotli] def mkStreamCompressor(): BrotliStreamCompressor = {
    new BrotliStreamCompressor( Brotli.DEFAULT_PARAMETER )
  }


  def compress: BrotliCompress = new BrotliCompress

}


object BrotliCompress extends MacroLogsImpl

/** Сжиматель данных из потока данных. */
class BrotliCompress extends GraphStage[FlowShape[ByteString, ByteString]] {

  import BrotliCompress.LOGGER

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

      private lazy val logPrefix = s"#${System.currentTimeMillis()}"

      private var streamCompressor = BrotliUtil.mkStreamCompressor()

      /** Close streamCompressor, if not closed. */
      private def _closeStreamComp(): Unit = {
        LOGGER.trace(s"$logPrefix Will close compressor $streamCompressor")
        if (streamCompressor != null) {
          LOGGER.trace(s"$logPrefix Closing compressor $streamCompressor")
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

          if (compressedBytea.isEmpty) {
            //LOGGER.trace(s"$logPrefix in.onPush() eat ${input.length}b")
            // Почти все отправки в brotli-компрессор заканчиваются пустым массивом на выходе: очень большое окно сжатия.
            pull(in)

          } else {
            // Иногда бывает, что компрессор что-то вернул. Надо пробросить вперёд и ждать следующего pull от downstream.
            LOGGER.trace(s"$logPrefix in.onPush().emit ${compressedBytea.length}b")
            val compressedOut = ByteString.fromArrayUnsafe( compressedBytea )
            emit(out, compressedOut)
          }
        }

        override def onUpstreamFinish(): Unit = {
          lazy val logPrefix2 = s"$logPrefix in.onUpstreamFinish()"
          try {
            val finalElement = ByteString.fromArrayUnsafe(
              streamCompressor.compressArray(Array.empty, true)
            )
            LOGGER.trace(s"$logPrefix2 Last: ${finalElement.length}b")
            if (finalElement.nonEmpty)
              emit(out, finalElement)
          } catch {
            case ex: Throwable =>
              LOGGER.error(s"$logPrefix2 Failed to flush stream", ex)
              throw ex
          } finally {
            _closeStreamComp()
          }
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          LOGGER.error(s"$logPrefix in: Upstream failure", ex)
          _closeStreamComp()
          super.onUpstreamFailure(ex)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          // По идее, hashBeenPulled всегда false. Тут просто защищаемся от возможных невероятных ситуаций.
          val hbp = hasBeenPulled(in)
          LOGGER.trace(s"$logPrefix onPull() hasBeenPulled?$hbp")
          if (!hbp)
            pull(in)
        }
      })

      override def postStop(): Unit = {
        LOGGER.trace(s"$logPrefix postStop()")
        super.postStop()
        // Убедиться, что streamCompressor закрыт.
        // В норме, он уже должен быть уже закрыт, но вдруг была какая-то внутренняя ошибка...
        _closeStreamComp()
      }

    }
  }

}

