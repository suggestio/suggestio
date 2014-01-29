package io.suggest.ym.cat

import io.suggest.ym.model.YmShopCategory
import scala.collection.mutable
import scala.annotation.tailrec
import io.suggest.ym.NormTokensOutAn
import YmCategory.{CAT_TRGM_MAP, CAT_TREE}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.14 14:30
 * Description: Для отображения пользовательского потока сознания на наши категории, надо анализировать и подбирать
 * категории. Модуль должен заниматься этим:
 * - Автоматически подбирать собственные категории магазина под дерево категорий маркета, чтобы
 *   все товары и услуги располагались в единой иерархии категорий suggest.io (маркета).
 * - Быстро и легко парсить удобное значение тега market_category.
 *
 * При генерации маппинга с shop category id на market_category используется анализ в несколько шагов:
 * Категории магазинов часто заданы в виде коротких "Автошины", "Транспортные услуги". Но бывают и более неоднозначные
 * "Мюющие средсва", которые могут относится к разным разделам категорий (например "для кухни" и "автохимия").
 * Чтобы понять, какую категорию выбрать, надо понять, чем в целом торгует магазин (загрузив в транслятор все категории
 * через learn()), затем определить основные разделы работы, и уже в этой области выбрать точные адреса неоднозначных
 * категорий.
 *
 * Для подавления других возможных ошибок автоматики, на уровне MR flow будет загружаться override-карта, в которой
 * оператор может составить карту cat_id -> market_category, чтобы исправить все возможные ошибки автоматики.
 * Но это уже за пределами этого модуля.
 */

object YmCatTranslator {
  /** Сколько отбирать категорий-кандидатов для маппинга максимум. (1) */
  val EARLY_PREFILTER_CAT_MAX = 3

  /** Отбрасывать кандидата, пережившего (1), если у того рейтинг ниже предыдущего кандидата, %. */
  val EARLY_CANDIDATE_FILTER_FALL = 0.49
}

import YmCatTranslator._

class YmCatTranslator {

  /** Внутренняя карта категорий: cat_id -> category. */
  protected val shopCatsMap = mutable.HashMap[String, YmShopCategory]()

  /** Обучение с помощью category datum.
    * @param shopCat Датум категории.
    */
  def learn(shopCat: YmShopCategory) {
    shopCatsMap(shopCat.id) = shopCat
  }

  /** Вызывается когда магазинных категорий больше нет. Возвращает карту cat_id -> market_category. */
  def getResultMap(implicit an: NormTokensOutAn) = {
    // Пора начать сбор данных по категориям. Сначала надо бегло прикинуть, какие категории куда пихать:
    // Нужно брать название вышестоящей категории и присоединять к текущей, затем делать триграммы.
    // Затем опрашивать словарь, и исходя из концентрации совпадений брать сверху несколько наиболее вероятных категорий.
    val shopCatsCandidates = shopCatsMap.map { case (shopCatId, shopCat) =>
      val fullPath = getFullCatPath(shopCat)
      val trgms = fullPath.foldLeft[Set[String]] (Set.empty) { (acc0, pathToken) =>
        val pathTokensNorm = an.toNormTokensRev(pathToken)
        YmCategory.getTrgmSet(pathTokensNorm, acc0)
      }
      // Пора опросить trgm-словарь на предмет наличия подходящих категорий.
      // Для аккамулятора используем словарь с рейтингами для категорий-кандидатов.
      val accMap = mutable.HashMap[YmCategoryT, Int]()
      trgms.foreach { trgm =>
        CAT_TRGM_MAP.get(trgm) match {
          case Some(mcCats) =>
            mcCats.foreach { mcCat =>
              val counter1 = accMap.get(mcCat) match {
                case Some(counter) => counter + 1
                case None          => 1
              }
              accMap(mcCat) = counter1
            }

          // Нет в словаре упоминаний такой последовательности символов. Явно, тут что-то астральное.
          case None =>  // Do nothing
        }
      }
      // accMap содержит инфу о категории и её рейтинге. Нужно высеять из карты лишние категории
      // -1 из to-индекса вычетать не надо из-за особенностей работы Seq.slice()
      val maxRating = trgms.size
      val catCandidates = accMap.toSeq
        // Сортировать по убыванию рейтинга (т.е. макс рейтинг - воглаве списка)
        .sortBy(_._2)
        .reverse
        // Надо выкинуть слишком "выпадающие" по рейтингу категории-кандидаты.
        .foldLeft[(List[(YmCategoryT, Float)], Int)] (Nil -> -1) {
          case (acc0 @ (accL0, lastCatRating), (cat, catRating)) =>
            if (catRating < lastCatRating * EARLY_CANDIDATE_FILTER_FALL) acc0 else {
              val catRatingNorm = catRating.toFloat / maxRating.toFloat
              val acc1 = cat -> catRatingNorm :: accL0
              acc1 -> catRating
            }
        }
        ._1
        .reverse                                // После foldLeft порядок был инвертирован
        .slice(0, EARLY_PREFILTER_CAT_MAX)      // Оставить не более указанного числа кандидатов.
      // Категории-кандидаты для текущей магазинной категории надо вернуть наверх
      shopCatId -> catCandidates
    }.toMap
    // Вторая стадия детектирования категорий. Надо теперь определить, в каких сегментах рынка магазин торгует.
    // Эти данным можно будет использовать для качественного уточнения выборки в спорных категориях.
    // Следует помнить, что любой магазин может также предоставлять какие-то "Услуги" и отражать это в прайсах.
    // Итак: для упрощения интересуемся только категориями первого уровня.
    val parentCatsMapAcc = mutable.HashMap[String, Int]()
    shopCatsCandidates.foreach { case (_, candidates) =>
      candidates.foreach { case (candidateCat, _) =>
        val parentId = candidateCat.topLevelCatName
        val v1 = parentCatsMapAcc.get(parentId) match {
          case Some(pRating) => pRating + 1
          case None          => 1
        }
        parentCatsMapAcc(parentId) = v1
      }
    }
    // Теперь есть на руках раскладка по сегментам рынка. Надо её пронормировать:
    val parentRatingTotal = parentCatsMapAcc.valuesIterator.sum.toFloat
    val parentCatsNormMap = parentCatsMapAcc.mapValues(_ / parentRatingTotal)
    val topCatName = parentCatsNormMap.maxBy { _._2 }._1
    val topCat = CAT_TREE / topCatName
    // Теперь можно накатить эту раскладку на списки кандидатов в shopCatsCandidates
    shopCatsCandidates.mapValues { candidatesList =>
      if (candidatesList.isEmpty) {
        topCat
      } else if (candidatesList.tail.isEmpty) {
        candidatesList.head._1
      } else {
        // Есть спорность в выборе категорий. Домножаем рейтинги категорий в списке на основное направление деятельности магазина.
        candidatesList
          .map { case (cat, catRating) =>
            val catRating1 = parentCatsNormMap(cat.topLevelCatName) * catRating
            cat -> catRating1
          }
          .sortBy { _._2 }
          .reverse
          .head
          ._1
      }
    }.toMap
    // TODO Ещё: магазинная подкатегория должна быть и подкатегорией (или той же самой категорией) в результирующем market_category.
    //      Группа магазинных подкатегорий может указать правильную категорию для своей над-категории.
  }


  @tailrec final protected def getFullCatPath(shopCat: YmShopCategory, acc: List[String] = Nil): List[String] = {
    val acc1 = shopCat.name :: acc
    val parentIdOpt = shopCat.parentIdOpt
    if (parentIdOpt.isDefined) {
      shopCatsMap.get(parentIdOpt.get) match {
        case Some(catParent) => getFullCatPath(catParent, acc1)
        case None => acc1
      }
    } else {
      acc1
    }
  }

}
