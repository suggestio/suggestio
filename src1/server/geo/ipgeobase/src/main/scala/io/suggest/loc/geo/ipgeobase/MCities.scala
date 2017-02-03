package io.suggest.loc.geo.ipgeobase

import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.geo.MGeoPoint
import io.suggest.geo.GeoPoint.Implicits._
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.{ExecutionContext, Future}

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
  implicit val FORMAT: OFormat[MCity] = (
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

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): MCity = {
    throw new UnsupportedOperationException("Deprecated API not implemented.")
  }

  import io.suggest.es.util.SioEsUtil._
  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldSource(enabled = true)
    )
  }

  import MCity.Fields._
  override def generateMappingProps: List[DocField] = {
    List(
      // Ничего не индексируется за ненадобность. При необходимости всегда можно пересоздать свежий индекс с иными параметрами.
      FieldNumber(CITY_ID_FN, fieldType = EsCityIdFieldType, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(NAME_FN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldString(REGION_FN, index = FieldIndexingVariants.no, include_in_all = true),
      FieldGeoPoint(CENTER_FN)
    )
  }

  /**
    * Реалтаймовый поиск по полю cityId.
    * На деле внутри используется getById(), что существенно ускоряет данный поиск.
    *
    * @param cityId целочисленный id городишки.
    * @return Фьючерс с опциональным инстансом [[MCity]].
    */
  def getByCityId(cityId: CityId_t): Future[Option[MCity]] = {
    val esId = MCity.cityId2esId(cityId)
    getById(esId)
  }

}


/**
  * Основная статическая модель для непосредственного взаимодействия с ней через обычное ES-модельное API.
  * Она всегда взаимодейтсвует с алиасом какого-либо индекса.
  */
@Singleton
class MCities @Inject() (
  mIndexes                : MIndexes,
  override val mCommonDi  : IEsModelDiVal
)
  extends MCitiesAbstract
{

  /** Используем алиас для последнего свежего индекса. */
  override def ES_INDEX_NAME = mIndexes.INDEX_ALIAS_NAME

}


/**
  * Реализация временно-используемой реализации статической модели,
  * работающую над индексом с произвольным (указанным) именем.
  */
class MCitiesTmp @Inject() (
  @Assisted override val ES_INDEX_NAME  : String,
  override val mCommonDi                : IEsModelDiVal
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

/** Реализация интерфейса jmx mbean'а [[MCitiesJmxMBean]]. */
final class MCitiesJmx @Inject() (
  override val companion    : MCities,
  override implicit val ec  : ExecutionContext
)
  extends EsModelJMXBaseImpl
  with MCitiesJmxMBean
{

  override type X = MCity

  override def getByCityId(cityId: CityId_t): String = {
    val strFut = for (mCityOpt <- companion.getByCityId(cityId)) yield {
      mCityOpt.toString
    }
    awaitString(strFut)
  }

}
