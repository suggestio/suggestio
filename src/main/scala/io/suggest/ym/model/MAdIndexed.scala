package io.suggest.ym.model

import io.suggest.model.inx2.{EsModelInx2StaticT, MInxT}
import ad._, common._
import io.suggest.model.{EsModelStaticMappingGenerators, EsModelEmpty}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.model.common._
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.util.SioConstants._
import io.suggest.util.JacksonWrapper
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.14 17:47
 * Description: Модель-наследница MMartAdIndexed, которая была перепилена в эту в связи со сменой
 * архитектуры.
 * Indexed-модель рекламы нужна в связи с гибким управлением отображением и поиском в определённых
 * контекстах. ES не умеет скрыто хранить данные в индексах (ex: неопубликованная рекламная карточка),
 * такое можно реализовать через фильтры, но присутствие отфильтрованных карточек всё равно будет
 * влиять на результаты поиска, и особенно на результаты competition suggester, который на момент
 * написания этого ещё не использовался.
 *
 * Эта модель работает через дополнительные параметр индекса, поэтому является кое-как совместимой
 * с EsModel, чего нельзя сказать об EsModelStatic, которая вообще не совместима с таким счастьем.
 *
 * Поле USER_CAT_STR оставляем тут, т.к. вынести его в трейт нормально не получится всё равно.
 */
object MAdIndexed
  extends EsModelInx2StaticT[MAdIndexed, MInxT]
  with EsModelStaticMappingGenerators
  with AdsSearchT[MAdIndexed, MInxT]
{

  val USER_CAT_STR_ESFN = "userCatStr"

  def generateMappingProps: List[DocField] = {
    FieldString(USER_CAT_STR_ESFN, include_in_all = true, boost = Some(0.5F), index = FieldIndexingVariants.no) ::
    MAd.generateMappingProps
  }

  def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = true, index_analyzer = EDGE_NGRAM_AN_1, search_analyzer = DFLT_AN),
    FieldSource(enabled = true)
  )


  protected def dummy(id: String, inx2: MInxT) = MAdIndexed(
    wrappedAd = MAd.dummy(id),
    userCatStr = null,
    showLevels = null,
    inx2 = inx2
  )


  def applyKeyValue(acc: MAdIndexed): PartialFunction[(String, AnyRef), Unit] = {
    // Собираем partial-функцию, которая будет всё делать как надо. Чтобы типы аккамуляторов и врапперов были совместимы, тут небольшой велосипед:
    val fm = MAd.applyKeyValue(acc.wrappedAd)
    val pf: PartialFunction[(String, AnyRef), Unit] = {
      case (USER_CAT_STR_ESFN, value)  => acc.userCatStr = JacksonWrapper.convert[List[String]](value)
      case other => fm(other)
    }
    pf
  }

}

import MAdIndexed.USER_CAT_STR_ESFN

/**
 * Экземпляр этой модели.
 * @param wrappedAd Исходная рекламная карточка [[MAd]].
 * @param userCatStr Текстовая информация о категории, нужна для полнотекстовой индексации.
 * @param showLevels Реальные уровни отображения.
 * @param inx2 Данные по индексу, нужны для сохранения/удаления. В индекс не сохраняются.
 */
case class MAdIndexed(
  wrappedAd        : MAd,
  var userCatStr   : List[String],
  override var showLevels: Set[AdShowLevel],
  inx2             : MInxT
)
  extends EsModelEmpty[MAdIndexed]
  with MAdWrapperT[MAd, MAdIndexed]
  // Свои поля
  with EMShowLevelsMut[MAdIndexed]
  // wrapped-поля (если изменяемы, то только напрямую)
  with EMProducerId[MAdIndexed]
  with EMAdOffers[MAdIndexed]
  with EMImg[MAdIndexed]
  with EMReceivers[MAdIndexed]
  with EMLogoImg[MAdIndexed]
  with EMTextAlign[MAdIndexed]
  with EMAdPanelSettings[MAdIndexed]
  with EMPrioOpt[MAdIndexed]
  with EMUserCatId[MAdIndexed]
  with EMDateCreated[MAdIndexed]
{
  /** EsModel-совместимого компаньона у этой модели нет, поэтому тут всегда будет ошибка. */
  @JsonIgnore
  def companion = throw new UnsupportedOperationException("MAdIndexed has no EsModelStatic-compatible companion. This is embrassing.")

  override def writeJsonFields(acc: XContentBuilder): Unit = {
    super.writeJsonFields(acc)
    if (!userCatStr.isEmpty)
      acc.array(USER_CAT_STR_ESFN, userCatStr : _*)
  }

  // Перезаписываем все методы, изначально работавшие через компаньона

  /** Узнать routing key для текущего экземпляра. Роутинга у этой модели нет. */
  override def getRoutingKey: Option[String] = None

  // TODO Надо бы работать со списками id, а не брать head от них. Это может вызвать проблемы и не совсем удобно.
  override protected def esIndexName: String = inx2.esInxNames.head
  override protected def esTypeName: String  = inx2.esTypes.head

  /**
   * Перед сохранением проверить, что есть хотя бы один уровень отображения.
   * Если уровней отображения нет, то сохранять такое добро нет никакого смысла, и этот
   * метод вызван по ошибке. */
  override def isFieldsValid: Boolean = {
    !showLevels.isEmpty && super.isFieldsValid
  }

  /**
   * Удалить текущий документ из хранилища. Если ключ не выставлен, то сразу будет экзепшен.
   * @return true - всё ок, false - документ не найден.
   */
  override def delete(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    id match {
      case Some(_id) => MAdIndexed.deleteAnyById(_id, inx2)
      case None => Future successful false
    }
  }

}

