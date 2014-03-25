package io.suggest.ym.index

import io.suggest.ym.model.YmOfferDatumFields._
import io.suggest.util.{SioEsUtil, SioConstants}, SioEsUtil._
import SioConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.02.14 17:41
 * Description: Тут утиль для описания индексов, которые хранят данные по магазинам и торговым центрам.
 */
object YmIndex {

  /** Сгенерить маппинг для индексов магазинов/торговых центров.
    * Структура соответствует YmOfferDatum.
    * @param typeName Название типа, для которого собираем маппинг. Обязательно.
    * @return XCOntentBuilder с готовым маппингом.
    */
  def getIndexMapping(typeName: String) = jsonGenerator { implicit b =>
    new IndexMapping(
      typ = typeName,

      staticFields = Seq(
        FieldSource(enabled = true),
        FieldAll(enabled = true)
      ),

      properties = Seq(
        FieldString(
          id = URL_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = OFFER_TYPE_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no    // Пока что маловероятно, что будет поиск по типу предложения.
        ),
        FieldString(
          id = GROUP_ID_ESFN,
          include_in_all = false,
          index = FieldIndexingVariants.no    // TODO Вероятно, следует как-то индексировать всё-таки.
        ),
        FieldBoolean(
          id = AVAILABLE_ESFN,
          include_in_all = false,
          // Не индексировать, т.к. можно через ранний filter всё это фильтровать.
          index = FieldIndexingVariants.no
        ),
        FieldString(
          id = SHOP_ID_ESFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = false
        ),
        FieldNumber(
          id = OLD_PRICES_ESFN,
          fieldType = DocFieldTypes.float,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNumber(
          id = PRICE_ESFN,
          fieldType = DocFieldTypes.float,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = CURRENCY_ID_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // market_category: Оно превращается в массив тегов.
        FieldString(
          id = MARKET_CATEGORY_ESFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = true
        ),
        // Список picture после FetchImageSA превращается в массив image_id.
        FieldString(
          id = "image_id",
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // Поля store, pickup и т.д. Хранятся as-is и не индексируются.
        FieldBoolean(
          id = STORE_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = PICKUP_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = DELIVERY_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = DELIVERY_INCLUDED_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNumber(
          id = LOCAL_DELIVERY_COST_ESFN,
          fieldType = DocFieldTypes.float,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // Прочие поля, описывающие любой товар.
        FieldString(
          id = DESCRIPTION_ESFN,
          index = FieldIndexingVariants.no,         // Фасет невозможен.
          include_in_all = true
        ),
        FieldString(
          id = SALES_NOTES_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = COUNTRY_OF_ORIGIN_ESFN,
          index = FieldIndexingVariants.analyzed,   // Фасет возможен.
          include_in_all = true
        ),
        FieldString(
          id = MANUFACTURER_WARRANTY_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = DOWNLOADABLE_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldBoolean(
          id = ADULT_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = AGE_ESFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),

        // Далее идут поля из payload
        FieldString(
          id = NAME_PFN,
          index = FieldIndexingVariants.no,       // Скорее всего, фасета тут нет и быть не может.
          include_in_all = true
        ),
        FieldString(
          id = VENDOR_PFN,
          index = FieldIndexingVariants.analyzed, // Тут будет фасет.
          include_in_all = true
        ),
        FieldString(
          id = VENDOR_CODE_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // Книги, диски. Нередко ищут по году.
        FieldNumber(
          id = YEAR_PFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = true
        ),
        // vendor.model
        FieldString(
          id = TYPE_PREFIX_PFN,
          index = FieldIndexingVariants.analyzed,   // Вполне вероятен фасет. Нередко тут вписана категория.
          include_in_all = true
        ),
        FieldString(
          id = MODEL_PFN,
          index = FieldIndexingVariants.no, // Наврядли будет фасет по модели
          include_in_all = true             // Но в поиске работать должно бы.
        ),
        FieldString(
          id = SELLER_WARRANTY_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // Тут Array[String] - список id рекомендованных товаров.
        // TODO Нужно бы разобраться с тем, как это дело приспособить к работе. Нужно offer_id задавать в _id документа.
        FieldString(
          id = REC_LIST_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // TODO Вероятно, можно индексировать вес, округляя его до 100 грамм.
        FieldNumber(
          id = WEIGHT_KG_PFN,
          fieldType = DocFieldTypes.float,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // TODO Тоже можно индексировать для поиска по фасетам. Нужно приводить к каким-то единицам измерения, округляя и индексировать.
        FieldString(
          id = EXPIRY_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = DIMENSIONS_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // *book
        FieldString(
          id = AUTHOR_PFN,
          index = FieldIndexingVariants.analyzed,
          include_in_all = true
        ),
        FieldString(
          id = PUBLISHER_PFN,
          index = FieldIndexingVariants.analyzed,
          include_in_all = true   // Не исключен поиск книги по издательству.
        ),
        FieldString(
          id = SERIES_PFN,
          index = FieldIndexingVariants.analyzed,
          include_in_all = true
        ),
        FieldString(
          id = ISBN_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNumber(
          id = VOLUMES_COUNT_PFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldNumber(
          id = VOLUME_PFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = true
        ),
        FieldString(
          id = LANGUAGE_PFN,
          index = FieldIndexingVariants.analyzed,
          include_in_all = true
        ),
        FieldString(
          id = TABLE_OF_CONTENTS_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // book
        FieldString(
          id = BINDING_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // TODO Возможно, тоже стоит фасет тут предусмотреть. Ведь люди любят искать книги типа "не толще чем 300 стр"
        FieldNumber(
          id = PAGE_EXTENT_PFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // audiobook
        FieldString(
          id = PERFORMED_BY_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true // Могут искать по читателю книги. Но это не фасет, пока что.
        ),
        FieldString(
          id = PERFORMANCE_TYPE_PFN,
          index = FieldIndexingVariants.analyzed,   // Фасет возможен.
          include_in_all = true
        ),
        FieldString(
          id = STORAGE_PFN,
          index = FieldIndexingVariants.analyzed,   // Фасет вероятен.
          include_in_all = true
        ),
        FieldString(
          id = FORMAT_PFN,
          index = FieldIndexingVariants.analyzed,   // Фасет вероятен.
          include_in_all = true
        ),
        FieldString(
          id = RECORDING_LEN_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // artist.title
        FieldString(
          id = COUNTRY_PFN,
          index = FieldIndexingVariants.no,  // Фасет возможен, но пока отключаем
          include_in_all = false
        ),
        FieldString(
          id = ARTIST_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = TITLE_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = MEDIA_PFN,
          index = FieldIndexingVariants.analyzed,     // Фасет вполне реален
          include_in_all = true
        ),
        FieldString(
          id = STARRING_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = DIRECTOR_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = ORIGINAL_NAME_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        // tour
        FieldString(
          id = WORLD_REGION_PFN,
          index = FieldIndexingVariants.analyzed,   // Фасет вполне вероятен.
          include_in_all = true
        ),
        FieldString(
          id = REGION_PFN,
          index = FieldIndexingVariants.analyzed,   // Возможен подфасет, т.е. после выбора WORLD_REGION
          include_in_all = true
        ),
        FieldNumber(
          id = DAYS_PFN,
          fieldType = DocFieldTypes.integer,
          index = FieldIndexingVariants.not_analyzed,   // Фасет: поиск по range
          include_in_all = true       // Бывают, что ищую "в грецию на 10 дней". TODO Надо только квантовать как-то период этот.
        ),
        // Сюда загружается Array[DateTime].
        FieldDate(
          id = TOUR_DATES_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        // Кол-во звёзд гостиницы задаётся во внутреннем формате: 5S, HV1 и т.д.
        // Это годится для фасетов, но не подходит для полнотекстового поиска.
        FieldString(
          id = HOTEL_STARS_PFN,
          index = FieldIndexingVariants.not_analyzed,   // Фасет возможен.
          include_in_all = false    // Искать в формате "греция 5 звёзд" пока можно только через доп.парсинг и генерацию подзапроса.
        ),
        // Тип гостиничной хаты. По формату - тоже самое.
        FieldString(
          id = HOTEL_ROOM_PFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = false
        ),
        // Тип жратвы. Формат задан в HotelMealTupes
        FieldString(
          id = HOTEL_MEAL_PFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = false
        ),
        FieldString(
          id = TOUR_INCLUDED_PFN,
          index = FieldIndexingVariants.no,   // Фасета тут нет и быть не может.
          include_in_all = true
        ),
        FieldString(
          id = TOUR_TRANSPORT_PFN,
          index = FieldIndexingVariants.no,   // Фасет возможен, но не ясно, что тут будет написано.
          include_in_all = true
        ),
        // event.ticket
        FieldString(
          id = ET_PLACE_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = ET_HALL_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldString(
          id = ET_HALL_PLAN_URL_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = false
        ),
        FieldString(
          id = ET_HALL_PART_PFN,
          index = FieldIndexingVariants.no,
          include_in_all = true
        ),
        FieldDate(
          id = ET_DATE_PFN,
          index = FieldIndexingVariants.analyzed,
          include_in_all = false    // Вероятно, произвольный поиск надо делать через
        ),
        FieldBoolean(
          id = ET_IS_PREMIERE_PFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = false
        ),
        FieldBoolean(
          id = ET_IS_KIDS_PFN,
          index = FieldIndexingVariants.not_analyzed,
          include_in_all = false
        )
      )
    )
  }

}
