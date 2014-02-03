package io.suggest.ym.cat

import io.suggest.ym.model.YmShopCategory
import scala.collection.mutable
import scala.annotation.tailrec
import io.suggest.ym.NormTokensOutAn
import YmCategory.{CAT_TRGM_MAP, CAT_TREE}
import io.suggest.util.{Lists, MacroLogsImplMin}
import io.suggest.util.MyConfig.CONFIG

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

object YmCatTranslator extends MacroLogsImplMin with Serializable {

  /** Тип карты-результата, который возвращается после окончания работы этой подсистемы.
    * Карта описывает, как сырой category_id надо отмаппить на какой элемент дерева категорий. */
  type ResultMap_t = Map[String, YmCategoryT]

  /** Если совпадение меньше чем на n%, то результат не считаем кандидатом (результат недостоверен). */
  val CAT_TRGM_SIM_MIN: Float = CONFIG.getFloat("ym.cat.trans.trgm.sim.min") getOrElse 0.25F

  /** Сколько отбирать категорий-кандидатов для маппинга максимум. (1) */
  val EARLY_PREFILTER_CAT_MAX: Int = CONFIG.getInt("ym.cat.trans.filter.early.count.max") getOrElse 15

  /** Отбрасывать кандидата, пережившего (1), если у того рейтинг ниже предыдущего кандидата, %. */
  val EARLY_CANDIDATE_FILTER_FALL: Float = CONFIG.getFloat("ym.cat.trans.candidate.fall.max") getOrElse 0.40F

  /** Когда категория и подкатегория одновременно идут на кандидатуры, то подкатегория может быть выброшена, если она близка по рейтингу. */
  val PARENT_CATEGORY_RATING_DOMINATION: Float = CONFIG.getFloat("ym.cat.trans.parent.rating.domination") getOrElse 1.10F

  /** Защита от длинных/бесконечных цепочек категорий. Максимальное количество категорий в дереве категорий. */
  val SHOP_CAT_MAX_PATH_LEN_DFLT: Int = CONFIG.getInt("ym.cat.trans.path.len.max") getOrElse 5

  /** Для сглаживания влияния сегментов рынка в выбор в спорных категориях, используется подавление роста через
    * возведение в степень, которая < 1 (т.е. взятие корня). */
  val PARENT_CAT_RATING_GROW_PRESSURE: Double = CONFIG.getDouble("ym.cat.trans.parent.grow.pressure") getOrElse 0.27

  /** Для категорий услуг нужно повышение приоритета, чтобы не терялись. */
  val SERVICE_CAT_INCR_REL: Float = CONFIG.getFloat("ym.cat.trans.parent.service.incr_by") getOrElse 1.5F

  /** Усиление рейтинга при последовательном совпадении последовательных trgm-токенов нормируется тут: */
  val SEQUENTAL_MATCH_NORM: Float = CONFIG.getFloat("ym.cat.trans.seq.match.norm") getOrElse 100F


  /**
   * Найти все индексы подстроки substr в указанной строке str.
   * По сути - рекурсивный враппер над [[java.lang.String.indexOf()]].
   * Функция полезна для уточнения позиций триграмм в исходных данных.
   * @param str Исходная строка.
   * @param substr Искомая подстрока.
   * @param acc Аккамулятор. По дефолту - пустой список.
   * @return Результирующий аккамулятор, содержащий список позиций подстроки от последнего к первому.
   *         Если ничего не найдено, то пустой список.
   */
  @tailrec private def tokenAllIndicesOfTrgm(str:String, substr:String, acc:List[Int] = Nil): List[Int] = {
    val startOffset = if (acc.isEmpty) 0 else acc.head + 1
    val nextOffset  = str.indexOf(substr, startOffset)
    if (nextOffset < 0) {
      acc
    } else {
      tokenAllIndicesOfTrgm(str, substr, nextOffset :: acc)
    }
  }

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
  def getResultMap(implicit an: NormTokensOutAn): ResultMap_t = {
    // Пора начать сбор данных по категориям. Сначала надо бегло прикинуть, какие категории куда пихать:
    // Нужно брать название вышестоящей категории и присоединять к текущей, затем делать триграммы.
    // Затем опрашивать словарь, и исходя из концентрации совпадений брать сверху несколько наиболее вероятных категорий.
    val shopCatsCandidates = shopCatsMap.map { case (shopCatId, shopCat) =>
      val fullPath = getFullCatPath(shopCat, acc=Nil, maxPathLen=SHOP_CAT_MAX_PATH_LEN_DFLT)
      val trgmsListRev = fullPath.foldLeft [List[String]] (Nil) { (acc0, pathToken) =>
        an.toNormTokensRev(pathToken).foldLeft(acc0) {
          (acc1, pathTokWord) => YmCategory.getTrgmList(pathTokWord, acc1)
        }
      }
      val trgms = trgmsListRev.toSet
      // Пора опросить trgm-словарь на предмет наличия подходящих категорий.
      // Для аккамулятора используем словарь с рейтингами для категорий-кандидатов.
      // TODO Нужно как-то усиливать инкремент для последовательных trgm-токенов. Если tokenAllIndicesOfTrgm() не понадобится для этого, то её можно удалить.
      val accMap = mutable.HashMap[YmCategoryT, Float]()
      trgms.foreach { trgm =>
        CAT_TRGM_MAP.get(trgm) match {
          case Some(mcCats) =>
            mcCats.foreach { mcCat =>
              val incr = 1F
              val counter1 = accMap.get(mcCat) match {
                case Some(counter) => counter + incr
                case None          => incr
              }
              accMap(mcCat) = counter1
            }

          // Нет в словаре упоминаний такой последовательности символов. Явно, тут что-то астральное.
          case None =>  // Do nothing
        }
      }
      // Нормируем
      val maxPossiblePoints = trgms.size.toFloat
      val accMap1 = accMap
        .mapValues { _ / maxPossiblePoints }   // Нормируем рейтинги категорий
        .filter { _._2 >= CAT_TRGM_SIM_MIN }   // Оставить только категории с рейтингом не ниже минимума.
            // Базовые сырые коэффициенты готовы. Надо теперь внести поправки на порядок токенов. Если trgm-токены совпадают по порядку: то это очень хорошо.
      val trgmsArrayRev = trgmsListRev.toArray
      val accMap2 = accMap1.foldLeft[List[(YmCategoryT, Float)]] (Nil) { case (acc, (cat, catRating)) =>
        val catPathRev = cat.pathRev
        val pathToken = catPathRev.head
        val pathTokenWordsRev = an.toNormTokensRev(pathToken)
        val catPathTokTrgmListRev = pathTokenWordsRev.reverse.foldLeft[List[String]] (Nil) {
          (trgmAcc, pathTokenWord) => YmCategory.getTrgmList(pathTokenWord, trgmAcc)
        }
        // TODO Надо использовать findRaggedLCS, но там бесконечный цикл и длинный Stack
        val lcs = Lists.findRaggedLCS(trgmsArrayRev, catPathTokTrgmListRev.toArray)
        val lcsLen = lcs.size - 1
        val catRatingIncr = lcsLen*lcsLen / SEQUENTAL_MATCH_NORM
        val catRating1 = catRating + catRatingIncr
        cat -> catRating1 :: acc
      } toMap
      val catCandidates = if (!accMap2.isEmpty) {
        val maxRating = accMap2.maxBy(_._2)._2
        // accMapNorm содержит инфу о категории и её рейтинге. Нужно высеять из карты лишние категории
        val allCandidatesMap = accMap2
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
    var parentCatsMapFloat = parentCatsMapAcc mapValues { count =>
      Math.pow(count, PARENT_CAT_RATING_GROW_PRESSURE).toFloat
    }
    //val parentRatingTotal = parentCatsMap1.valuesIterator.sum
    val (topCatName, topCatRating) = parentCatsMapFloat.maxBy { _._2 }
    parentCatsMapFloat = parentCatsMapFloat.mapValues { _ / topCatRating }
    // Для "услуг" немного повышаемый рейтинг, чтобы не терялись среди товаров.
    val serviceCatInfoOpt = parentCatsMapFloat.get(YmCategory.SERVICE_CAT_NAME)
    if (serviceCatInfoOpt.isDefined) { 
      parentCatsMapFloat = parentCatsMapFloat.updated(YmCategory.SERVICE_CAT_NAME, serviceCatInfoOpt.get * SERVICE_CAT_INCR_REL)
    }
    // Когда нет кандидатов в категории, надо выбрать основную категорию магазина
    val topCat = CAT_TREE / topCatName
    // Теперь можно накатить эту раскладку на списки кандидатов в shopCatsCandidates.
    // Используем map() вместо mapValues(), чтобы иметь доступ к текущей категории для нужд логгера.
    shopCatsCandidates.map { case (catName, candidatesList) =>
      val v1 = if (candidatesList.isEmpty) {
        debug(s"$catName: No candidates found. Returning default '$topCat'.")
        topCat
      } else if (candidatesList.tail.isEmpty) {
        candidatesList.head._1
      } else {
        // Есть спорность в выборе категорий. Домножаем рейтинги категорий в списке на основное направление деятельности магазина.
        candidatesList
          .map { case (cat, catRating) =>
            val catRating1 = parentCatsMapFloat(cat.topLevelCatName) * catRating
            cat -> catRating1
          }
          .sortBy { _._2 }
          .reverse
          .head
          ._1
      }
      catName -> v1
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
