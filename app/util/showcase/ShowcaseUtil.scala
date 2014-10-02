package util.showcase

import io.suggest.ym.model.MAd
import io.suggest.ym.model.common.{AdShowLevels, IBlockMeta}
import models.{AdSearch, BlockConf, MMartCategory}
import util.blocks.BlocksConf
import play.api.Play.{current, configuration}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:10
 * Description:
 */
object ShowcaseUtil {

  /** Отображать ли пустые категории? */
  val SHOW_EMPTY_CATS = configuration.getBoolean("market.frontend.cats.empty.show") getOrElse false

  def getCatOwner(adnId: String) = MMartCategory.DEFAULT_OWNER_ID

  /**
   * Сгруппировать "узкие" карточки, чтобы они были вместе.
   * @param ads Исходный список элементов.
   * @tparam T Тип элемента.
   * @return
   */
  def groupNarrowAds[T <: IBlockMeta](ads: Seq[T]): Seq[T] = {
    val (enOpt1, acc0) = ads.foldLeft [(Option[T], List[T])] (None -> Nil) {
      case ((enOpt, acc), e) =>
        val blockId = e.blockMeta.blockId
        val bc: BlockConf = BlocksConf(blockId)
        if (bc.isNarrow) {
          enOpt match {
            case Some(en) =>
              (None, en :: e :: acc)
            case None =>
              (Some(e), acc)
          }
        } else {
          (enOpt, e :: acc)
        }
    }
    val acc1 = enOpt1 match {
      case Some(en) => en :: acc0
      case None     => acc0
    }
    acc1.reverse
  }


  /**
   * Сбор данных по категорям для выдачи указанного узла-ресивера.
   * @param adnIdOpt id узла, если есть.
   * @return Кортеж из фьючерса с картой статистики категорий и списком отображаемых категорий.
   */
  def getCats(adnIdOpt: Option[String]) = {
    val catAdsSearch = AdSearch(
      receiverIds   = adnIdOpt.toList,
      maxResultsOpt = Some(100),
      levels        = List(AdShowLevels.LVL_CATS)
    )
    // Сборка статитстики по категориям нужна, чтобы подсветить серым пустые категории.
    val catsStatsFut = MAd.stats4UserCats(MAd.dynSearchReqBuilder(catAdsSearch))
      .map { _.toMap }
    // Текущие категории узла
    val mmcatsFut: Future[Seq[MMartCategory]] = if(SHOW_EMPTY_CATS) {
      // Включено отображение всех категорий.
      val catOwnerId = adnIdOpt.fold(MMartCategory.DEFAULT_OWNER_ID) { getCatOwner }
      for {
        mmcats <- MMartCategory.findTopForOwner(catOwnerId)
        catsStats <- catsStatsFut
      } yield {
        // Нужно, чтобы пустые категории шли после непустых. И алфавитная сортировка
        val nonEmptyCatIds = catsStats
          .iterator
          .filter { case (_, count)  =>  count > 0L }
          .map { _._1 }
          .toSet
        mmcats.sortBy { mmcat =>
          val sortPrefix: String = nonEmptyCatIds.contains(mmcat.idOrNull) match {
            case true  => "a"
            case false => "z"
          }
          sortPrefix + mmcat.name
        }
      }
    } else {
      // Отключено отображение скрытых категорий. Исходя из статистики, прочитать из модели только необходимые карточки.
      catsStatsFut flatMap { catsStats =>
        MMartCategory.multiGet(catsStats.keysIterator)
          .map { _.sortBy(MMartCategory.sortByMmcat) }
      }
    }
    (catsStatsFut, mmcatsFut)
  }

}

