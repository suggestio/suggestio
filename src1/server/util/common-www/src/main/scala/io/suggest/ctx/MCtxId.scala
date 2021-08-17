package io.suggest.ctx

import java.util.UUID
import java.util.regex.Pattern

import javax.inject.Inject
import io.suggest.sec.{HmacAlgos, HmacUtil}
import io.suggest.util.UuidUtil
import io.suggest.util.logs.MacroLogsImplLazy
import io.suggest.xplay.psb.PathBindableImpl
import io.suggest.xplay.qsb.AbstractQueryStringBindable
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
final class MCtxIds @Inject() (
                                configuration: Configuration
                              )
  extends MacroLogsImplLazy
{

  /** Ключ для подписи. */
  private lazy val SECRET_KEY = configuration.get[String]( MCtxId.SECKET_KEY_CONF_NAME )


  /** Посчитать сигнатуру для указанного UUID-ключа. */
  def keyUuid2sig(keyUuid: UUID, personIdOpt: Option[String]): String = {
    val sigMac = HmacUtil.mkMac( HmacAlgos.HMAC_SHA1, SECRET_KEY )
    sigMac.update( UuidUtil.uuidToBytes(keyUuid) )

    // Отработать значение personId
    for (personId <- personIdOpt) {
      sigMac.update( MCtxId.TO_STRING_SEP.getBytes() )
      sigMac.update( personId.getBytes() )
    }

    val macBytes = sigMac.doFinal()
    Base64.encodeBase64URLSafeString( macBytes )
  }


  /** Вернуть рандомный валидный инстанс. */
  def apply(personIdOpt: Option[String]): MCtxId = {
    val keyUuid = UUID.randomUUID()
    MCtxId(
      key       = UuidUtil.uuidToBase64( keyUuid ),
      personId  = personIdOpt,
      sig       = keyUuid2sig( keyUuid, personIdOpt )
    )
  }


  /** Проверка валидности инстанса. */
  def checkSig(m: MCtxId): Boolean = {
    try {
      val keyUuid = UuidUtil.base64ToUuid( m.key )
      val sig = keyUuid2sig( keyUuid, m.personId )
      m.sig ==* sig

    } catch {
      case ex: Throwable =>
        LOGGER.error(s"verify($m): Failed verify instance", ex)
        false
    }
  }


  /** Проверка валидности инстанса для текущего юзера. */
  def validate(m: MCtxId, personIdOpt: Option[String]): Boolean = {
    checkSig(m) && personIdOpt ==* m.personId
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
                                key       : String,
                                personId  : Option[String],
                                sig       : String
                              ) {

  override def toString: String =
    MCtxId.intoString( this )

}


object MCtxId extends MacroLogsImplLazy {

  private[ctx] def TO_STRING_SEP = "."

  /** Сериализация инстанса [[MCtxId]] в строку. */
  def intoString(m: MCtxId): String = {
    val s = TO_STRING_SEP
    s"${m.key}$s${m.personId.getOrElse("")}$s${m.sig}"
  }


  /** Десериализация из строки. */
  def fromString(s: String): Option[MCtxId] = {
    try {
      // TODO Opt Использовать indexOf() + substring() вместо регэкспов?
      Pattern.quote(TO_STRING_SEP).r.split(s) match {
        case Array(key, personIdOrEmpty, sig) =>
          val personIdOpt = if (personIdOrEmpty.isEmpty) {
            None
          } else {
            Some( personIdOrEmpty )
          }
          Some( MCtxId(key, personIdOpt, sig) )
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
    new AbstractQueryStringBindable[MCtxId] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MCtxId]] = {
        for (ctxIdStrE <- strB.bind(key, params)) yield {
          ctxIdStrE.flatMap { ctxStr =>
            _fromStringOpt2eith(
              fromString(ctxStr)
            )
          }
        }
      }

      override def unbind(key: String, value: MCtxId): String = {
        strB.unbind(key, intoString(value))
      }

    }
  }

  private def _fromStringOpt2eith( fromStringRes: Option[MCtxId] ) = {
    fromStringRes
      .toRight("e.ctxid.fromString")
  }

  /** Поддержка биндинга значения из URL path. */
  implicit def mCtxIdPathBindable(implicit strB: PathBindable[String]): PathBindable[MCtxId] = {
    new PathBindableImpl[MCtxId] {
      override def bind(key: String, value: String): Either[String, MCtxId] = {
        _fromStringOpt2eith {
          fromString(value)
        }
      }

      override def unbind(key: String, value: MCtxId): String = {
        intoString(value)
      }
    }
  }


  /** Поддержка UnivEq. */
  @inline implicit def univEq: UnivEq[MCtxId] = UnivEq.derive

  /** Название параметра конфига с секретным ключиком для сигнатуры sig. */
  private[ctx] def SECKET_KEY_CONF_NAME = "ctx.id.sig.secret"

}
