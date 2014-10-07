package models

import models.im.ImOp
import play.api.Play.{current, configuration}
import play.api.mvc.QueryStringBindable
import util.img.ImgIdKey
import util.qsb.QsbSigner
import util.PlayLazyMacroLogsImpl

import scala.annotation.tailrec
import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 17:55
 * Description: Аргументы запроса динамической картинки. Эти аргументы могут включать в себя размер
 * картинки, кроп, качество сжатия и др. Сериализованный набор аргументов подписывается ключом сервера,
 * чтобы избежать модификации аргументов на клиенте.
 * Контроллер, получая эти аргументы, должен выдать картинку, или произвести её на основе оригинала.
 */
object DynImgArgs extends PlayLazyMacroLogsImpl {

  import LOGGER._

  private val SIGN_SUF   = ".sig"
  private val IMG_ID_SUF = ".id"

  /** Статический секретный ключ для подписывания запросов к dyn-картинкам. */
  private val SIGN_SECRET: String = {
    val confKey = "dynimg.sign.key"
    configuration.getString(confKey) getOrElse {
      if (play.api.Play.isProd) {
        // В продакшене без ключа нельзя. Генерить его и в логи писать его тоже писать не стоит наверное.
        throw new IllegalStateException(s"""Production mode without dyn-img signature key defined is impossible. Please define '$confKey = ' like 'application.secret' property with 64 length.""")
      } else {
        // В devel/test-режимах допускается использование рандомного ключа.
        val rnd = new Random()
        val len = 64
        val sb = new StringBuilder(len)
        // Избегаем двойной ковычи в ключе, дабы не нарываться на проблемы при копипасте ключа в конфиг.
        @tailrec def nextPrintableCharNonQuote: Char = {
          val next = rnd.nextPrintableChar()
          if (next == '"' || next == '\\')
            nextPrintableCharNonQuote
          else
            next
        }
        for(i <- 1 to len) {
          sb append nextPrintableCharNonQuote
        }
        val result = sb.toString()
        warn(s"""Please define secret key for dyn-img cryto-signing in application.conf:\n  $confKey = "$result" """)
        result
      }
    }
  }

  /** routes-биндер для query-string. */
  implicit def qsb(implicit iikB: QueryStringBindable[ImgIdKey],  imOpsOptB: QueryStringBindable[Option[Seq[ImOp]]]) = {
    new QueryStringBindable[DynImgArgs] {

      /** Создать подписывалку для qs. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key$SIGN_SUF")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DynImgArgs]] = {
        // Собираем результат
        val keyDotted = s"$key."
        for {
          // TODO Надо бы возвращать invalid signature при ошибке, а не not found.
          params2         <- getQsbSigner(key).signedOrNone(keyDotted, params)
          maybeImgId      <- iikB.bind(key + IMG_ID_SUF, params2)   // И сразу запрещаем содержимое cropOpt:
            .map { _.right.filter(_.cropOpt.isEmpty) getOrElse Left("Already croppped imgs are not supported.") }
          maybeImOpsOpt   <- imOpsOptB.bind(keyDotted, params2)
        } yield {
          maybeImgId.right.flatMap { imgId =>
            maybeImOpsOpt.right.map { imOpsOpt =>
              val imOps = imOpsOpt.getOrElse(Nil)
              DynImgArgs(imgId, imOps)
            }
          }
        }
      }

      override def unbind(key: String, value: DynImgArgs): String = {
        val imgIdRaw = iikB.unbind(key + IMG_ID_SUF, value.imgId)
        val imOpsUnbinded = imOpsOptB.unbind(s"$key.", if (value.imOps.isEmpty) None else Some(value.imOps))
        val unsignedResult = if (imOpsUnbinded.isEmpty) {
          imgIdRaw
        } else {
          imgIdRaw + "&" + imOpsUnbinded
        }
        getQsbSigner(key).mkSigned(key, unsignedResult)
      }
    }
  }

}


/**
 * Контейнер с распарсенными результатами.
 * @param imgId ID (rowkey) картинки, производную от которой надо получить.
 * @param imOps трансформации, которые необходимо сделать.
 */
case class DynImgArgs(
  imgId       : ImgIdKey,
  imOps       : Seq[ImOp]
) {

  def imOpsToStringLossy: String = {
    ImOp.unbindImOps("", imOps, withOrderInx = false)
  }

}

