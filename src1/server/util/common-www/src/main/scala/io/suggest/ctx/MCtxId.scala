package io.suggest.ctx

import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

import io.suggest.model.play.psb.PathBindableImpl
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sec.{HmacAlgos, HmacUtil}
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImplLazy
import org.apache.commons.codec.binary.Base64
import play.api.Configuration
import japgolly.univeq._
import play.api.mvc.{PathBindable, QueryStringBindable}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.17 10:31
  * Description: String-модель для неизменяемого ctxId.
  * Прошлый ctxId был просто псевдо-случайной строкой и жила прямо внутри Context.
  * Но это не защищало от возможного перебора значений.
  *
  * Модель [[MCtxId]] защищает ctxId от модификаций с помощью сигнатуры.
  */
class MCtxIds @Inject() (
                          configuration: Configuration
                        )
  extends MacroLogsImplLazy
{

  /** Ключ для подписи. */
  private val SECRET_KEY = configuration.get[String]( MCtxId.SECKET_KEY_CONF_NAME )


  /** Посчитать сигнатуру для указанного UUID-ключа. */
  def keyUuid2sig(key: String): String = {
    val keyUuid = UuidUtil.base64ToUuid( key )
    keyUuid2sig( keyUuid )
  }
  def keyUuid2sig(keyUuid: UUID): String = {
    val sigMac = HmacUtil.mkMac( HmacAlgos.HMAC_SHA1, SECRET_KEY )
    sigMac.update( UuidUtil.uuidToBytes(keyUuid) )
    val macBytes = sigMac.doFinal()
    Base64.encodeBase64URLSafeString( macBytes )
  }

  /** Вернуть рандомный валидный инстанс. */
  def apply(): MCtxId = {
    val keyUuid = UUID.randomUUID()
    MCtxId(
      key = UuidUtil.uuidToBase64( keyUuid ),
      sig = keyUuid2sig( keyUuid )
    )
  }


  /** Проверка валидности инстанса. */
  def verify(m: MCtxId): Boolean = {
    try {
      val sig = keyUuid2sig( m.key )
      m.sig ==* sig

    } catch {
      case ex: Throwable =>
        LOGGER.error(s"verify($m): Failed verify instance", ex)
        false
    }
  }


}


/** Модель уникального контекстного ключа и сигнатуры для защиты ключа от изменений.
  * В отличие от QsbSigner, тут явная сигнатура и только ручная проверка, т.е. валидность
  * инстанса надо проверять в контроллерах самостоятельно через [[MCtxIds]].verify().
  *
  * @param key Ключ на базе UUID.
  * @param sig Сигнатура.
  */
case class MCtxId private[ctx](
                                key  : String,
                                sig  : String
                              ) {
  override def toString = MCtxId.intoString( this )
}


object MCtxId extends MacroLogsImplLazy {

  object QsFields {
    def KEY_FN = "k"
    def SIG_FN = "s"
  }


  private def TO_STRING_SEP = "."

  /** Сериализация инстанса [[MCtxId]] в строку. */
  def intoString(m: MCtxId): String = {
    s"${m.key}$TO_STRING_SEP${m.sig}"
  }


  private val SPLIT_RE = Pattern.quote(TO_STRING_SEP).r

  /** Десериализация из строки. */
  def fromString(s: String): Option[MCtxId] = {
    try {
      // TODO Opt Использовать indexOf() + substring() вместо регэкспов?
      SPLIT_RE.split(s) match {
        case Array(key, sig) =>
          Some( MCtxId(key, sig) )
        case other =>
          LOGGER.warn(s"fromString(): Cannot parse string: $s ;; after split => $other")
          None
      }
    } catch {
      case ex: Throwable =>
        LOGGER.error(s"fromString(): Failed to parse string: $s", ex)
        None
    }
  }


  /** Поддержка биндинга значения из URL qs. */
  implicit def mCtxIdQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MCtxId] = {
    new QueryStringBindableImpl[MCtxId] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MCtxId]] = {
        val k = key1F( key )
        val F = QsFields
        for {
          keyE <- strB.bind( k(F.KEY_FN), params )
          sigE <- strB.bind( k(F.SIG_FN), params )
        } yield {
          for {
            key <- keyE.right
            sig <- sigE.right
          } yield {
            MCtxId(
              key = key,
              sig = sig
            )
          }
        }
      }

      override def unbind(key: String, value: MCtxId): String = {
        val k = key1F( key )
        val F = QsFields
        _mergeUnbinded1(
          strB.unbind( k(F.KEY_FN), value.key ),
          strB.unbind( k(F.SIG_FN), value.sig )
        )
      }

    }
  }


  /** Поддержка биндинга значения из URL path. */
  implicit def mCtxIdPathBindable(implicit strB: PathBindable[String]): PathBindable[MCtxId] = {
    new PathBindableImpl[MCtxId] {
      override def bind(key: String, value: String): Either[String, MCtxId] = {
        fromString(value)
          .toRight("e.ctxid.fromString")
      }

      override def unbind(key: String, value: MCtxId): String = {
        intoString(value)
      }
    }
  }


  /** Поддержка UnivEq. */
  implicit def univEq: UnivEq[MCtxId] = UnivEq.derive

  /** Название параметра конфига с секретным ключиком для сигнатуры sig. */
  private[ctx] def SECKET_KEY_CONF_NAME = "ctx.id.sig.secret"

}
