package io.suggest.model.n2.extra.domain

import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.text.util.UrlUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.16 17:34
  * Description: Если узел слинкован с внешним сайтом, то тут описываются домен (домены) и настройки интеграции с ними.
  * Что-то подобное было в mnesia-моделях suggest.io live search.
  *
  * Изначально появился для поддержки обратной интеграции домена на уровне DNS.
  * Т.е. когда запросы на сторонний хост отрабатывает выдача s.io.
  *
  * dkey -- Доменный ключ (dkey по терминологии suggest.io live search).
  * Это очищенное нормализованное и почищенное доменное имя без всяких www. вначале.
  * Например: "suggest.io".
  */
object MDomainExtra
  extends IEsMappingProps
{

  /** ES-названия полей живут здесь. */
  object Fields {
    val DKEY_FN = "d"
    val MODE_FN = "m"
  }


  /** Поддержка JSON. */
  implicit val domainExtraJson: OFormat[MDomainExtra] = {
    val F = Fields
    (
      (__ \ F.DKEY_FN).format[String] and
      (__ \ F.MODE_FN).format[MDomainMode]
    )(apply, unlift(unapply))
  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.DKEY_FN -> FKeyWord.indexedJs,
      F.MODE_FN -> FKeyWord.indexedJs,
    )
  }

  import play.api.data.Mapping
  import play.api.data.Forms._

  def mappingM: Mapping[MDomainExtra] = {
    mapping(
      "dkey" -> nonEmptyText
        .transform[String](UrlUtil.host2dkey, identity),
      "mode" -> MDomainMode.mapping
    )
    { MDomainExtra.apply }
    { MDomainExtra.unapply }
  }

}


/** Класс для экземпляров модели.
  *
  * @param dkey Ключ домена.
  * @param mode Режим интеграции по модели [[MDomainModes]].
  */
case class MDomainExtra(
  dkey    : String,
  mode    : MDomainMode
) {

  /** Стараться использовать https вместо http? */
  def preferSecured: Boolean = false

  def proto: String = {
    if (preferSecured)
      "https"
    else
      "http"
  }

}
