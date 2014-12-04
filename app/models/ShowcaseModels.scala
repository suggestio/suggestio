package models

import models.blk.SzMult_t
import play.api.mvc.QueryStringBindable

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.14 16:58
 * Description: Файл с моделями, которые относятся к выдаче маркета.
 */

/** Результат работы детектора текущего узла. */
case class GeoDetectResult(ngl: NodeGeoLevel, node: MAdnNode)


/**
 * Найденные узлы с одного геоуровня. Используеся в ctl.MarketShowcase для списка узлов.
 * @param nodes Список узлов в порядке отображения.
 * @param nameOpt messages-код отображаемого слоя. Если None, то не будет ничего отображено.
 * @param expanded Отображать уже развёрнутов? false по умолчанию.
 */
case class GeoNodesLayer(
  nodes: Seq[MAdnNode],
  ngl: NodeGeoLevel,
  nameOpt: Option[String] = None,
  expanded: Boolean = false
)


/**
 * Контейнер неготовых асинхронных результатов работы [[util.showcase.ShowcaseUtil.getCats()]].
 * @param catsStatsFut Фьючерс со статистиками категорий.
 * @param catsFut Фьючерс со списком категорий
 */
case class GetCatsResult(
  catsStatsFut  : Future[Map[String, Long]],
  catsFut       : Future[Seq[MMartCategory]]
) {

  def future(implicit ec: ExecutionContext): Future[GetCatsSyncResult] = {
    for {
      catsStats <- catsStatsFut
      cats      <- catsFut
    } yield {
      GetCatsSyncResult(catsStats, cats)
    }
  }

}

/**
 * Контейнер только готовых результатов работы [[util.showcase.ShowcaseUtil.getCats()]].
 * @param catsStats Статистики категорий.
 * @param cats Список категорий.
 */
case class GetCatsSyncResult(
  catsStats  : Map[String, Long],
  cats       : Seq[MMartCategory]
)


/**
 * Для URL css'ки, относящейся к конкретной карточке, нужно передать эти параметры:
 * @param adId id рекламной карточки.
 * @param szMult Мульпликатор размера.
 */
case class AdCssArgs(
  adId    : String,
  szMult  : SzMult_t
) {
  override def toString: String = {
    // TODO Нужно укорачивать szMult: 1.0 -> 1
    adId + AdCssArgs.SEP_RE + szMult
  }
}

object AdCssArgs {

  val SEP_RE = ",".r

  def fromString(s: String) = {
    val Array(adId, szMultStr) = SEP_RE.split(s)
    AdCssArgs(adId, szMultStr.toFloat)
  }

}
