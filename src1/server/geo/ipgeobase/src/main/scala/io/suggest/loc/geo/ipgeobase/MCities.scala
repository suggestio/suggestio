package io.suggest.loc.geo.ipgeobase

import com.google.inject.assistedinject.Assisted
import io.suggest.es.MappingDsl
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.geo.MGeoPoint
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.09.16 15:52
  * Description: Модель городов, фигурирующих в БД ipgeobase.
  *
  * Модель ориентирована на возможность работы с несколькими ES-индексами,
  * поэтому тут есть две статических модели: [[MCities]] и [[MCitiesTmp]].
  * Вторая используется только при обновлении БД ipgeobase.
  */
object MCity {

  /** Контейнер с именами полей. */
  object Fields {
    def CITY_ID_FN  = "cid"
    def NAME_FN     = "n"
    def REGION_FN   = "r"
    def CENTER_FN   = "g"
  }

  def cityId2esId(cityId: CityId_t): String = {
    cityId.toString
  }

  import Fields._

  /** Поддержка JSON. */
  implicit def cityJson: OFormat[MCity] = (
    (__ \ CITY_ID_FN).format[CityId_t] and
    (__ \ NAME_FN).format[String] and
    (__ \ REGION_FN).formatNullable[String] and
    (__ \ CENTER_FN).format[MGeoPoint]
  )(apply, unlift(unapply))

}


/** Класс, содержащих всю общую логику статических моделей [[MCities]]. */
abstract class MCitiesAbstract
  extends EsModelStatic
  with EsmV2Deserializer
  with EsModelJsonWrites
  with MacroLogsImpl
{

  override type T = MCity
  override def ES_TYPE_NAME = "city"

  // Неявно обращаемся к MCity.FORMAT.
  override def esDocReads(meta: IEsDocMeta) = implicitly[Reads[T]]
  override def esDocWrites = implicitly[OWrites[T]]

  /** Сборка маппинга индекса по новому формату. */
  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    val F = MCity.Fields
    IndexMapping(
      source = Some( FSource(someTrue) ),
      properties = Some( Json.obj(
        F.CITY_ID_FN -> FNumber(
          typ   = DocFieldTypes.Short,
          index = someFalse,
        ),
        F.NAME_FN   -> FText.notIndexedJs,
        F.REGION_FN -> FText.notIndexedJs,
        F.CENTER_FN -> FGeoPoint.indexedJs,
      ))
    )
  }

}


/**
  * Основная статическая модель для непосредственного взаимодействия с ней через обычное ES-модельное API.
  * Она всегда взаимодейтсвует с алиасом какого-либо индекса.
  */
@Singleton
class MCities
  extends MCitiesAbstract
{

  /** Используем алиас для последнего свежего индекса. */
  override def ES_INDEX_NAME = MIndexes.INDEX_ALIAS_NAME

}


/**
  * Реализация временно-используемой реализации статической модели,
  * работающую над индексом с произвольным (указанным) именем.
  */
class MCitiesTmp @Inject() (
  @Assisted override val ES_INDEX_NAME  : String,
)
  extends MCitiesAbstract

/** Интерфейс для Guice factory, которая занимается сборкой временных инстансов этой модели. */
trait MCitiesTmpFactory {
  def create(esIndexName: String): MCitiesTmp
}


/**
  * Класс элементов модели [[MCities]].
  * @param cityId city_id по спискам ipgeobase.
  * @param cityName Название города.
  * @param region Название региона.
  * @param center Координаты центра города.
  */
case class MCity(
  cityId    : CityId_t,
  cityName  : String,
  region    : Option[String],
  center    : MGeoPoint
)
  extends EsModelT
{
  override def versionOpt = None
  override def id: Option[String] = {
    val esId = MCity.cityId2esId(cityId)
    Some(esId)
  }
}


/** Интерфейс JMX MBean'а для модели [[MCities]]. */
trait MCitiesJmxMBean extends EsModelJMXMBeanI {

  def getByCityId(cityId: CityId_t): String

}

@Singleton
class MCitiesModel @Inject()(
                              esModel: EsModel
                            ) {

  import esModel.api._

  object api {
    implicit class MCitiesAbstractOpsExt( model: MCitiesAbstract ) {

      /**
        * Реалтаймовый поиск по полю cityId.
        * На деле внутри используется getById(), что существенно ускоряет данный поиск.
        *
        * @param cityId целочисленный id городишки.
        * @return Фьючерс с опциональным инстансом [[MCity]].
        */
      def getByCityId(cityId: CityId_t): Future[Option[MCity]] = {
        val esId = MCity.cityId2esId(cityId)
        model.getById(esId)
      }

    }
  }

}

/** Реализация интерфейса jmx mbean'а [[MCitiesJmxMBean]]. */
final class MCitiesJmx @Inject() (
                                   mCitiesModel              : MCitiesModel,
                                   override val companion    : MCities,
                                   override val esModelJmxDi : EsModelJmxDi,
                                 )
  extends EsModelJMXBaseImpl
  with MCitiesJmxMBean
{

  import esModelJmxDi.ec
  import mCitiesModel.api._
  import io.suggest.util.JmxBase._

  override type X = MCity

  override def getByCityId(cityId: CityId_t): String = {
    val strFut = for {
      mCityOpt <- companion.getByCityId(cityId)
    } yield {
      mCityOpt.toString
    }
    awaitString(strFut)
  }

}
