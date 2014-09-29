package models

import play.api.Play.{current, configuration}
import play.api.mvc.QueryStringBindable
import util.qsb.QsbSigner

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.09.14 17:55
 * Description: Аргументы запроса динамической картинки. Эти аргументы могут включать в себя размер
 * картинки, кроп, качество сжатия и др. Сериализованный набор аргументов подписывается ключом сервера,
 * чтобы избежать модификации аргументов на клиенте.
 * Контроллер, получая эти аргументы, должен выдать картинку, или произвести её на основе оригинала.
 */
object DynImgArgs {

  private val SIGN_SUF   = ".s"
  private val IMG_ID_SUF = ".id"
  private val SZ_SUF     = ".sz"

  private val SIGN_SECRET: String = configuration.getString("dynimg.sign.key").get

  /** routes-биндер для query-string. */
  def qsb(implicit strB: QueryStringBindable[String], miimB: QueryStringBindable[Option[MImgInfoMeta]]) = {
    new QueryStringBindable[DynImgArgs] {

      /** Создать подписывалку для qs. */
      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, s"$key.$SIGN_SUF")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DynImgArgs]] = {
        // Собираем результат
        for {
          // TODO Надо бы возвращать invalid signature при ошибке, а не not found.
          params2     <- getQsbSigner(key).signedOrNone(s"$key.", params)
          maybeImgId  <- strB.bind(key + IMG_ID_SUF, params2)
          maybeSz     <- miimB.bind(key + SZ_SUF, params2)
        } yield {
          maybeImgId.right.flatMap { imgId =>
            maybeSz.right.map { szOpt =>
              DynImgArgs(
                imgId = imgId,
                sz    = szOpt
              )
            }
          }
        }
      }

      override def unbind(key: String, value: DynImgArgs): String = {
        val imgIdRaw = strB.unbind(key + IMG_ID_SUF, value.imgId)
        val sb = new StringBuilder(imgIdRaw)
        if (value.sz.isDefined) {
          sb.append('&')
            .append(miimB.unbind(key + SZ_SUF, value.sz))
        }
        val unsignedResult = sb.toString()
        getQsbSigner(key).mkSigned(key, unsignedResult)
      }
    }
  }

}


import DynImgArgs._


/**
 *
 * @param imgId ID (rowkey) картинки, производную от которой надо получить.
 * @param sz Размер картинки по вертикали и горизонтали.
 */
case class DynImgArgs(
  imgId   : String,
  sz      : Option[MImgInfoMeta] = None
)
