package io.suggest.ym.cat

import io.suggest.ym.model.YmShopCategory
import scala.collection.mutable
import scala.annotation.tailrec
import io.suggest.ym.NormTokensOutAn
import YmCategory.{CAT_TRGM_MAP, CAT_TREE}
import io.suggest.util.LogsImpl

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

object YmCatTranslator extends Serializable {
  private val LOGGER = new LogsImpl(classOf[YmCatTranslator])

  /** Если совпадение меньше чем на n%, то результат не считаем кандидатом (результат недостоверен). */
  val CAT_TRGM_SIM_MIN = 0.25F

  /** Сколько отбирать категорий-кандидатов для маппинга максимум. (1) */
  val EARLY_PREFILTER_CAT_MAX = 15

  /** Отбрасывать кандидата, пережившего (1), если у того рейтинг ниже предыдущего кандидата, %. */
  val EARLY_CANDIDATE_FILTER_FALL = 0.40F

  /** Когда категория и подкатегория одновременно идут на кандидатуры, то подкатегория может быть выброшена, если она близка по рейтингу. */
  val PARENT_CATEGORY_RATING_DOMINATION = 1.10F

  val SHOP_CAT_MAX_PATH_LEN_DFLT = 5
}

import YmCatTranslator._


class YmCatTranslator extends Serializable {
  import LOGGER._

  /** Внутренняя карта категорий: cat_id -> category. */
  protected val shopCatsMap = mutable.HashMap[String, YmShopCategory]()

  /** Обучение с помощью category datum.
    * @param shopCat Датум категории.
    */
  def learn(shopCat: YmShopCategory) {
    shopCatsMap(shopCat.id) = shopCat
  }

  /** Вызывается когда магазинных категорий больше нет. Возвращает карту cat_id -> market_category.
    * @param an Неявно передаваемый текстовый анализатор.
    * @return Карту для маппинга исходный id категорий на валидные market_category.
    */
  def getResultMap(implicit an: NormTokensOutAn): Map[String, YmCategoryT] = {
    // Пора начать сбор данных по категориям. Сначала надо бегло прикинуть, какие категории куда пихать:
    // Нужно брать название вышестоящей категории и присоединять к текущей, затем делать триграммы.
    // Затем опрашивать словарь, и исходя из концентрации совпадений брать сверху несколько наиболее вероятных категорий.
    val shopCatsCandidates = shopCatsMap.map { case (shopCatId, shopCat) =>
      val fullPath = getFullCatPath(shopCat, acc=Nil, maxPathLen=SHOP_CAT_MAX_PATH_LEN_DFLT)
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
      val maxPossiblePoints = trgms.size.toFloat
      val accMapNorm = accMap
        .mapValues { _ / maxPossiblePoints }   // Нормируем рейтинги категорий
        .filter { _._2 >= CAT_TRGM_SIM_MIN }   // Оставить только категории с рейтингом не ниже минимума.
      val catCandidates = if (!accMapNorm.isEmpty) {
        val maxRating = accMapNorm.maxBy(_._2)._2
        // accMapNorm содержит инфу о категории и её рейтинге. Нужно высеять из карты лишние категории
        val allCandidatesMap = accMapNorm
          // Если спад рейтинга слишком резок относительно лидера, то тоже отсеиваем
          .filter { case (_, catRating)  =>  catRating >= maxRating * EARLY_CANDIDATE_FILTER_FALL }
          // На след.шаге нужен доступ ко всем категориям по их идентификаторам
          .map { case e @ (cat, _)  =>  cat.pathRev -> e }
        // Если в кандидатурах есть категория и её подкатегория, и они близки по рейтингу, то нужно выкинуть одну из них.
        if (!allCandidatesMap.isEmpty) {
          allCandidatesMap
            .filter { case (_, (cat, catRating)) =>
              allCandidatesMap.get(cat.parentPathRev) match {
                // Нет родительской категории в списках. Волноваться не о чем.
                case None => true
                // Есть родительская категория в карте кандидатов.
                case Some((parentCat, parentCatRating)) =>
                  // Если рейтинг категории близок к родительскому или ниже его, то надо выкинуть эту подкатегорию из кандидатов
                  val result = catRating > parentCatRating * PARENT_CATEGORY_RATING_DOMINATION
                  trace(s"Parent2child cat.filter: Found parent category: ${cat.pathStr} $catRating ==>> ${parentCat.pathStr} $parentCatRating :: keepChild=$result")
                  result
              }
            }
            .toSeq                  // Отбросить карту и врЕменные ключи, которые создавались ради предыдущего фильтра.
            .map(_._2)
            .sortBy(_._2)                       // Нужно оставить только несколько лидирующих кандидатов. Сортируем по убыв.
            .reverse
            .slice(0, EARLY_PREFILTER_CAT_MAX)  // -1 из to-индекса вычетать не надо из-за особенностей работы Seq.slice()
        } else {
          Nil
        }
      } else {
        Nil
      }
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


  /** Вычисление полного поути shop-категории.
    * @param shopCat Исходная категория.
    * @param acc Аккамулятор названий категорий.
    * @param maxPathLen Максимальная длина пути в дереве. Позволяет справится с рядом возможных атак.
    * @return Список токенов в пути в прямом порядке.
    */
  @tailrec final protected def getFullCatPath(shopCat: YmShopCategory, acc: List[String], maxPathLen: Int): List[String] = {
    val shopCatId = shopCat.id
    lazy val logPrefix = s"getFullCatPath($shopCatId): "
    val acc1 = shopCat.name :: acc
    if (maxPathLen <= 0) {
      trace(logPrefix + "Too long category path: stopping traverse. Current path is " + acc)
      acc1
    } else {
      val parentIdOpt = shopCat.parentIdOpt
      if (parentIdOpt.isDefined) {
        // Запрещаем parent_id указывать на саму себя.
        val parentId = parentIdOpt.get
        if (parentId == shopCatId) {
          trace(logPrefix + "Suppressed parent category self-reference: " + shopCatId)
          acc1
        } else {
          // Проверка на валидность parent_id делается в YmlSax, но тут мы её также делаем.
          shopCatsMap.get(parentId) match {
            case Some(catParent) =>
              getFullCatPath(catParent, acc1, maxPathLen - 1)

            // id категории есть, но он указывает вникуда. Игнорируем.
            case None =>
              trace(s"${logPrefix}parent_id=$parentId is invalid: no such category. Ignoring.")
              acc1
          }
        }
      } else {
        acc1
      }
    }
  }

}
