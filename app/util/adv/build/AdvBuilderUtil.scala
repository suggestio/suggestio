package util.adv.build

import com.google.inject.{Inject, Singleton}
import io.suggest.geo.MGeoPoint
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.edge.MNodeEdges
import models.MPredicate
import models.adv.build.MCtxOuter
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.adv.geo.tag.GeoTagsUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 21:35
  * Description: Утиль для AdvBuilder'а.
  */
@Singleton
class AdvBuilderUtil @Inject() (
  geoTagsUtil : GeoTagsUtil,
  mCommonDi   : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.driver.api.Query


  /**
    * Подготовка данных и внешнего контекста для билдера, который будет содержать дополнительные данные,
    * необходимые для работы внутри самого билдера.
    *
    * @param itemsSql Заготовка запроса поиска
    * @return Фьючерс с outer-контекстом для дальнейшей передачи его в билдер.
    */
  def prepareInstallNew(itemsSql: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    geoTagsUtil.prepareInstallNew(itemsSql)
  }


  /**
    * Окончание инсталляции новых item'ов.
    * @param ctxOuterFut Результат prepareInstallNew().
    * @return Фьючер без полезных данных внутри.
    */
  def afterInstallNew(ctxOuterFut: Future[MCtxOuter]): Future[_] = {
    geoTagsUtil.afterInstallNew(ctxOuterFut)
  }


  /**
    * Подготовка outer-контекста к деинсталляции item'ов, требующих дополнительных действий.
    *
    * @param itemsSql Выборка item'ов, которые будут деинсталлированы.
    * @return Фьючерс с готовым outer-контекстом.
    */
  def prepareUnInstall(itemsSql: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    geoTagsUtil.prepareUnInstall(itemsSql)
  }

  def afterUnInstall(ctxOuterFut: Future[MCtxOuter]): Future[_] = {
    geoTagsUtil.afterUnInstall(ctxOuterFut)
  }


  def clearByPredicate(b0: IAdvBuilder, preds: Seq[MPredicate]): IAdvBuilder = {
    // Вычистить теги из эджей карточки
    val acc2Fut = for {
      acc0 <- b0.accFut
    } yield {
      val mad2 = acc0.mad.copy(
        edges = acc0.mad.edges.copy(
          out = {
            val iter = acc0.mad
              .edges
              // Все теги и геотеги идут через биллинг. Чистка равносильна стиранию всех эджей TaggedBy.
              .withoutPredicateIter( preds: _* )
            MNodeEdges.edgesToMap1( iter )
          }
        )
      )
      // Сохранить почищенную карточку в возвращаемый акк.
      acc0.copy(
        mad = mad2
      )
    }
    b0.withAcc( acc2Fut )
  }


  /** Извлечение геоточек из MItems для нужд статистики.
    *
    * @param mitems Итемы биллинга или же итератор оных.
    * @return Итератор из награбленных точек.
    */
  def grabGeoPoints4Stats(mitems: TraversableOnce[MItem]): Iterator[MGeoPoint] = {
    mitems
      .toIterator
      .flatMap(_.geoShape)
      .map { gs =>
        gs.centerPoint
          // Плевать, если не центральная точка: в работе самой геолокации это не используется, только для всякой статистики.
          .getOrElse( gs.firstPoint )
      }
  }

}

/** Интерфейс для DI-поля, содержащего инстанс [[AdvBuilderUtil]]. */
trait IAdvBuilderUtilDi {
  def advBuilderUtil: AdvBuilderUtil
}
