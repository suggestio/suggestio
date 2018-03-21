package io.suggest.sec.av

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.ByteString
import io.suggest.async.AsyncUtil
import io.suggest.streams.ByteStringsChunker
import io.suggest.util.logs._
import japgolly.univeq._
import play.api.Configuration

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
                                  asyncUtil                 : AsyncUtil,
                                  configuration             : Configuration,
                                  implicit private val actorSystem  : ActorSystem,
                                  implicit private val mat          : Materializer,
                                  implicit private val ec           : ExecutionContext
                                )
  extends MacroLogsImpl
{

  private def TCP_CONFIG = configuration.getOptional[Configuration]( "sec.av.clam.tcp" )

  private val CLAM_REMOTE_HOST_PORT: Option[(String, Int)] = {
    for {
      avConf <- TCP_CONFIG
      host   <- avConf.getOptional[String]("host")
    } yield {
      val port = avConf.getOptional[Int]("port").getOrElse(3310)
      (host, port)
    }
  }

  /** Максимальный размер одной порции исходного файла, отправляемого по TCP. */
  private def TCP_CHUNK_SIZE = 2048

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
    val src = FileIO.fromPath( new File(req.file).toPath )
    scanClamdRemoteTcp(src)
  }


  private def tcpCharset = StandardCharsets.US_ASCII

  /** Рендер целого число в байты. */
  private def int2byteString(i: Int): ByteString = {
    val bbuf = ByteBuffer.allocate(4).putInt(i).array()
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
    val connectionFlow = Tcp()
      .outgoingConnection(clamdHostPort._1, clamdHostPort._2)

    // TODO Если clamd начал ругаться во время отправки, то надо прерывать дальнейшую отправку.

    // Надо нарезать исходник на кусочки, добавив размер каждого chunk'а перед ним.
    val scanCmdSrc = Source
      .single( ByteString.fromString("zINSTREAM\u0000", tcpCharset) )
      .concat {
        src
          .via( ByteStringsChunker(TCP_CHUNK_SIZE) )
          .filter(_.nonEmpty)
          .map { chunk =>
            // Формат такой: <dataLength:4><data:dataLength>
            val lenBs = int2byteString( chunk.length )
            // Приклеить байты длины к chunk'у данных и вернуть:
            lenBs ++ chunk
          }
      }
      .concat(
        Source.single( int2byteString( 0 ) )
      )

    // Clamd возвращает сообщения прямо во время приёма файла (по мере выполнения проверки).
    val scanOutputSink = Flow[ByteString]
      .map(_.utf8String)
      .toMat( Sink.seq )(Keep.right)

    val (_, resFut) = connectionFlow.runWith(scanCmdSrc, scanOutputSink)

    for (res <- resFut) yield {
      LOGGER.trace(s"scanClamdRemoteTcp(): Response =\n${res.mkString}")
      val isClean = res.forall { resPart =>
        println(resPart)
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
