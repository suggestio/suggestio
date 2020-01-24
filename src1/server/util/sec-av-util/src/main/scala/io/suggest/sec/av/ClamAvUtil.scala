package io.suggest.sec.av

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import javax.inject.Inject
import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl._
import akka.util.ByteString
import io.suggest.async.AsyncUtil
import io.suggest.util.logs._
import japgolly.univeq._
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.02.18 12:05
  * Description: Утиль для сканирования файлов антивирусом clam.
  * Используется для проверки принимаемых файлов.
  *
  * Есть два варианта проверки:
  * - Быстрый локальный через unix-сокет (clamdscan --fdpass).
  * - Обычный, с передачей файла по tcp.
  */
final class ClamAvUtil @Inject()(
                                  injector: Injector,
                                )
  extends MacroLogsImpl
{
  private lazy val asyncUtil = injector.instanceOf[AsyncUtil]
  private lazy val configuration = injector.instanceOf[Configuration]
  implicit private lazy val actorSystem = injector.instanceOf[ActorSystem]
  implicit private lazy val mat = injector.instanceOf[Materializer]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  private lazy val CLAM_REMOTE_HOST_PORT: Option[(String, Int)] = {
    for {
      avConf <- configuration.getOptional[Configuration]( "sec.av.clam.tcp" )
      host   <- avConf.getOptional[String]("host")
    } yield {
      val port = avConf.getOptional[Int]("port").getOrElse(3310)
      LOGGER.info(s"Configured remote clamd: $host:$port")
      (host, port)
    }
  }

  LOGGER.debug(s"Clam remote = ${CLAM_REMOTE_HOST_PORT.orNull}")

  object Words {
    final def CLAMDSCAN = "clamdscan"
    final def FDPASS    = "--fdpass"
  }

  /** API для проверки файла антивирусом.
    *
    * @param req Описание данных для сканера.
    * @return Фьючерс с результатом проверки.
    */
  def scan(req: ClamAvScanRequest): Future[IClamAvScanResult] = {
    CLAM_REMOTE_HOST_PORT.fold[Future[IClamAvScanResult]] {
      Future {
        scanClamdFdpass(req)
      }( asyncUtil.singleThreadCpuContext )
    } { _ =>
      scanClamdRemote(req)
    }
  }


  /** Быстрое простое сканирование антивирусом с помощью консольной утилиты clamdscan.
    *
    * @param req Описание данных для сканирования.
    * @return Фьючерс с результатом.
    */
  def scanClamdFdpass(req: ClamAvScanRequest): ClamdShellScanResult = {
    lazy val logPrefix = s"scanClamdFdpass()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix $req")

    import Words._
    val ret = Process(CLAMDSCAN :: FDPASS :: req.file :: Nil) ! LOGGER.process(logPrefix)
    ClamdShellScanResult(ret)
  }


  def scanClamdRemote(req: ClamAvScanRequest): Future[ClamAvScanResult] = {
    LOGGER.trace(s"scanClamdRemote(): File = ${req.file}")
    // Используем lazy-source, чтобы файл не читался, пока сокет не будет открыт:
    val src = Source.lazySource { () =>
      FileIO.fromPath(
        new File(req.file).toPath,
        chunkSize = 8192
      )
    }
    scanClamdRemoteTcp(src)
  }


  private def tcpCharset = StandardCharsets.US_ASCII

  /** Рендер целого число в байты. */
  private def int2byteString(i: Int): ByteString = {
    val bbuf = ByteBuffer
      .allocate(4)
      //.order( ByteOrder.BIG_ENDIAN )
      .putInt(i)
      .array()
    ByteString.fromArrayUnsafe( bbuf )
  }


  /** Неблокирующее сканирование с помощью clamd с помощью INSTREAM.
    *
    * @param src Источник (читаемый файл).
    * @param clamdHostPort Данные для связи с демоном clamd.
    * @return Фьючерс с результатом скана.
    */
  def scanClamdRemoteTcp(src: Source[ByteString, _],
                         clamdHostPort: (String, Int) = CLAM_REMOTE_HOST_PORT.get): Future[ClamAvScanResult] = {
    lazy val logPrefix = s"scanClamdRemoteTcp()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix Will stream data to $clamdHostPort")
    val connectionFlow = Tcp()
      .outgoingConnection(clamdHostPort._1, clamdHostPort._2)

    // TODO Если clamd начал ругаться во время отправки, то надо прерывать дальнейшую отправку.

    // Надо нарезать исходник на кусочки, добавив размер каждого chunk'а перед ним.
    val scanCmdSrc = Source
      .single( ByteString.fromString("zINSTREAM\u0000", tcpCharset) )
      .concat {
        src
          // Здесь раньше был ByteStringChunker, но он не нужен для файлов, а по для остальных лучше самостоятельно это разрулить (на уровне каждого конкретного source).
          .filter(_.nonEmpty)
          .map { chunk =>
            // Формат такой: <dataLength:4><data:dataLength>
            val l = chunk.length
            val lenBs = int2byteString( l )
            LOGGER.trace(s"$logPrefix $l bytes: $lenBs")
            // Приклеить байты длины к chunk'у данных и вернуть:
            lenBs ++ chunk
          }
      }
      .concat(
        Source.single( int2byteString( 0 ) )
      )
      // Допускаем некоторое склеивание кусочков на уровне исходящих tcp-пакетов, если исходник читается быстро:
      .buffer(8, OverflowStrategy.backpressure)

    // Clamd возвращает сообщения прямо во время приёма файла (по мере выполнения проверки).
    val scanOutputSink = Flow[ByteString]
      .map(_.utf8String)
      .toMat( Sink.seq )(Keep.right)

    val (_, resFut) = connectionFlow.runWith(scanCmdSrc, scanOutputSink)

    // TODO Определять ошибки clamav.
    // TODO Нужен таймаут после завершения отсылки. clamav закрывает сокет по таймауту через минуту-две, но это долговато.
    for (res <- resFut) yield {
      LOGGER.trace(s"scanClamdRemoteTcp(): Response =\n${res.mkString}")
      val isClean = res.forall { resPart =>
        resPart.contains("OK") && !resPart.contains("FOUND")
      }
      ClamAvScanResult(isClean)
    }
  }

}


/** Модель-контейнер аргументов запроса AV-проверки.
  *
  * @param file Проверяемый файл.
  */
case class ClamAvScanRequest(
                              file: String
                            )


/** Интерфейс модели любого результата запроса AV-проверки. */
trait IClamAvScanResult {
  def isClean: Boolean
}
case class ClamAvScanResult(isClean: Boolean) extends IClamAvScanResult


/** Модель-контейнер аргументов результата AV-проверки через консольную clamdscan.
  *
  * @param result Код результата, возвращённый консольной утилитой.
  */
case class ClamdShellScanResult(
                            result   : Int
                          )
  extends IClamAvScanResult
{

  def isClean: Boolean = {
    result ==* 0
  }

}

