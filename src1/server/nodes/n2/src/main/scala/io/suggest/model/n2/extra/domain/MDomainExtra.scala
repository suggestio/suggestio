package io.suggest.model.n2.extra.domain

import io.suggest.es.model.IGenEsMappingProps
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
object MDomainExtra extends IGenEsMappingProps {

  /** ES-названия полей живут здесь. */
  object Fields {
    val DKEY_FN = "d"
    val MODE_FN = "m"
  }


  import Fields._

  /** Поддержка JSON. */
  implicit val FORMAT: OFormat[MDomainExtra] = (
    (__ \ DKEY_FN).format[String] and
    (__ \ MODE_FN).format[MDomainMode]
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  /** Описание полей суб-модели для elasticsearch. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(DKEY_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldString(MODE_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }


  import play.api.data.Mapping
  import play.api.data.Forms._

  def mappingM: Mapping[MDomainExtra] = {
    mapping(
      "dkey" -> nonEmptyText,    // TODO dkey verify, normalize
      "mode" -> MDomainModes.mapping
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
