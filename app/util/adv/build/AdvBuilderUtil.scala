package util.adv.build

import com.google.inject.{Inject, Singleton}
import io.suggest.mbill2.m.item.typ.MItemTypes
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
  import slick.driver.api._
  import LOGGER._


  /**
    * Подготовка данных и внешнего контекста для билдера, который будет содержать дополнительные данные,
    * необходимые для работы внутри самого билдера.
    *
    * @param itemQuery0 Заготовка запроса поиска
    * @return
    */
  def prepareInstallNew(itemQuery0: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    lazy val logPrefix = s"prepareInstallNew(${System.currentTimeMillis}):"

    for {
      // Найти все теги, которые затрагиваются грядующим инсталлом.
      tagFacesOpts <- slick.db.run {
        itemQuery0
          .filter(_.iTypeStr === MItemTypes.GeoTag.strId)
          .map(_.tagFaceOpt)
          .distinct
          .result
      }

      // Создать множество недублирующихся тегов.
      tagFaces = {
        val r = tagFacesOpts
          .iterator
          .flatMap(_.iterator)
          .toSet
        trace(s"$logPrefix Found ${r.size} tag faces")
        r
      }

      // Собрать карту узлов-тегов, создав при необходимости какие-то новые узлы-теги.
      gtMap <- geoTagsUtil.ensureTags(tagFaces)

    } yield {
      trace(s"$logPrefix Have Map[tagFace,node] with ${gtMap.size} keys.")
      // Собрать и вернуть результат.
      MCtxOuter(
        tagFacesNodesMap = gtMap
      )
    }
  }


  def clearByPredicate(b0: IAdvBuilder, pred: MPredicate): IAdvBuilder = {
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
              .withoutPredicateIter( pred )
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

}

/** Интерфейс для DI-поля, содержащего инстанс [[AdvBuilderUtil]]. */
trait IAdvBuilderUtilDi {
  def advBuilderUtil: AdvBuilderUtil
}
