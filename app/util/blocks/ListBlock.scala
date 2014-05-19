package util.blocks

import play.api.data._, Forms._
import models._
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:02
 * Description: Утиль для блоков, которые содержат в себе списки офферов вместо одного оффера.
 * Почти всегда это блоки title+price.
 */

/** Для сборки блоков, обрабатывающие блоки с офферами вида "title+price много раз", используется этот трейт. */
trait TitlePriceListBlockT extends ValT {
  // Названия используемых полей.
  val TITLE_FN = "title"
  val PRICE_FN = "price"

  /** Начало отсчета счетчика офферов. */
  val N0 = 0

  /** Макс кол-во офферов (макс.длина списка офферов). */
  def offersCount: Int

  protected def bfTitle(offerNopt: Option[Int]) = BfText(TITLE_FN, BlocksEditorFields.TextArea, maxLen = 128, offerNopt = offerNopt)
  protected def bfPrice(offerNopt: Option[Int]) = BfPrice(PRICE_FN, offerNopt = offerNopt)

  /** Генерация описания полей. У нас тут повторяющийся маппинг, поэтому blockFields для редактора генерится без полей-констант. */
  abstract override def blockFieldsRev: List[BlockFieldT] = {
    val acc0 = super.blockFieldsRev
    (N0 until offersCount).foldLeft(acc0) {
      (acc, offerN) =>
        val offerNopt = Some(offerN)
        val titleBf = bfTitle(offerNopt)
        val priceBf = bfPrice(offerNopt)
        priceBf :: titleBf :: acc
    }
  }

  // Поля оффера
  protected def titleMapping = bfTitle(None)
  protected def priceMapping = bfPrice(None)

  // Маппинг для одного элемента (оффера)
  protected def offerMapping = tuple(
    titleMapping.getOptionalStrictMappingKV,
    priceMapping.getOptionalStrictMappingKV
  )
  // Маппинг для списка офферов.
  protected def offersMapping = list(offerMapping)
    .verifying("error.too.much", { _.size <= offersCount })
    .transform[List[AOBlock]] (applyAOBlocks, unapplyAOBlocks)


  /** Собрать AOBlock на основе куска выхлопа формы. */
  protected def applyAOBlocks(l: List[(Option[AOStringField], Option[AOPriceField])]): List[AOBlock] = {
    l.iterator
      // Делаем zipWithIndex перед фильтром чтобы сохранять выравнивание на странице (css-классы), если 1 или 2 элемент пропущен.
      .zipWithIndex
      // Выкинуть пустые офферы
      .filter {
      case ((titleOpt, priceOpt), _) =>
        titleOpt.isDefined || priceOpt.isDefined
    }
      // Оставшиеся офферы завернуть в AOBlock
      .map {
      case ((titleOpt, priceOpt), i) =>
        AOBlock(n = i,  text1 = titleOpt,  price = priceOpt)
    }
      .toList
  }

  /** unapply для offersMapping. Вынесен для упрощения кода. Метод восстанавливает исходный выхлоп формы,
    * даже если были пропущены какие-то группы полей. */
  protected def unapplyAOBlocks(aoBlocks: Seq[AOBlock]) = {
    // без if isEmpty будет экзепшен в maxBy().
    if (aoBlocks.isEmpty) {
      Nil
    } else {
      // Вычисляем оптимальную длину списка результатов
      val maxN = aoBlocks.maxBy(_.n).n
      // Рисуем карту маппингов необходимой длины, ключ - это n.
      val aoBlocksNS = aoBlocks
        .map { aoBlock => aoBlock.n -> aoBlock }
        .toMap
      // Восстанавливаем новый список выхлопов мапперов на основе длины и имеющихся экземпляров AOBlock.
      (N0 to maxN)
        .map { n =>
        aoBlocksNS
          .get(n)
          .map { aoBlock => aoBlock.text1 -> aoBlock.price }
          .getOrElse(None -> None)
      }
        .toList
    }
  }

  // Mapping
  private val m = offersMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeOffers = m.bind(data)
    (maybeAcc0, maybeOffers) match {
      case (Right(acc0), Right(offers)) =>
        acc0.offers = offers
        maybeAcc0

      case (Left(accFE), Right(descr)) =>
        maybeAcc0

      case (Right(_), Left(colorFE)) =>
        Left(colorFE)   // Избыточная пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(colorFE)) =>
        Left(accFE ++ colorFE)
    }
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.bd.offers )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.bd.offers
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

