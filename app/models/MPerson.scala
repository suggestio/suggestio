package models

import io.suggest.model.{EsModelStaticT, EsModelT}
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.common.xcontent.XContentBuilder
import play.api.Play.current
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import io.suggest.model.EsModel._
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте. Как правило, админ одного или нескольких сайтов.
 * Люди различаются по email'ам и/или по номерам телефона.
 * 01.03.2014 Вместо hbase использовать EsModel. Добавлен phone. id вместо email теперь
 * генерится силами ES.
 */

// Статическая часть модели.
object MPerson extends EsModelStaticT[MPerson] {

  val ES_TYPE_NAME = "person"

  val LANG_ESFN   = "lang"
  val IDENTS_ESFN = "idents"

  // Список емейлов админов suggest.io.
  val SU_IDS: Set[String] = {
    // id'шники суперюзеров sio можно указыват через конфиг, но мыльники должны быть в известных доменах.
    current.configuration.getStringSeq("sio.superuser.emails")
      .map {
        _.view.filter {
          email => email.endsWith("@cbca.ru") || email.endsWith("@shuma.ru")
        }.toSet
      }
      .getOrElse(Set(
        "konstantin.nikiforov@cbca.ru",
        "ilya@shuma.ru",
        "sasha@cbca.ru",
        "maksim.sharipov@cbca.ru"
      ))
  }

  /**
   * Принадлежит ли указанный мыльник суперюзеру suggest.io?
   * @param email емейл.
   * @return true, если это почта админа. Иначе false.
   */
  def isSuperuserId(email: String) = SU_IDS contains email

  /** Сгенерить маппинг для индекса. */
  def generateMapping: XContentBuilder = jsonGenerator { implicit b =>
    IndexMapping(
      typ = ES_TYPE_NAME,
      static_fields = Seq(
        FieldAll(enabled = false, analyzer = FTS_RU_AN),
        FieldSource(enabled = true)
      ),
      properties = Seq(
        FieldString(
          id = LANG_ESFN,
          index = FieldIndexingVariants.analyzed,
          include_in_all = false
        )
      )
    )
  }


  def applyKeyValue(acc: MPerson): PartialFunction[(String, AnyRef), Unit] = {
    case (LANG_ESFN, value)     => acc.lang = stringParser(value)
  }

  protected def dummy(id: String) = MPerson(id = Some(id), lang = null)
}

import MPerson._

/**
 * Экземпляр модели MPerson.
 * @param lang Язык интерфейса для указанного пользователя.
 *             Формат четко неопределён, и соответствует коду выхлопа Controller.lang().
 */
case class MPerson(
  var lang  : String,
  var id    : Option[String] = None
) extends EsModelT[MPerson] with MPersonLinks {

  def personId = id.get

  override def companion = MPerson

  override def writeJsonFields(acc: XContentBuilder) {
    acc.field(LANG_ESFN, lang)
  }
}


/** Трайт ссылок с юзера для других моделей. */
trait MPersonLinks {
  def personId: String

  @JsonIgnore def person(implicit ec:ExecutionContext, client: Client) = {
    MPerson getById personId
  }
  @JsonIgnore def isSuperuser = MPerson isSuperuserId personId
  def authzForDomain(dkey: String) = MPersonDomainAuthz.getForPersonDkey(dkey, personId)
  @JsonIgnore def allDomainsAuthz = MPersonDomainAuthz.getForPerson(personId)
}


