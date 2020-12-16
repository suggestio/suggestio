package io.suggest.es

import enumeratum.values._
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, OptionUtil}
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.2019 18:17
  * Description: Класс DSL-синтаксиса для сборки ES-маппингов индексов, который по сути статический.
  * Куча object'ов и val'ов, которые нужны только при запуске, будут вычищены следом за инстансом этого класс.
  */

final class MappingDsl { dsl =>

  val someTrue = OptionUtil.SomeBool.someTrue
  val someFalse = OptionUtil.SomeBool.someFalse

  private object Util {
    val typ = (__ \ "_type").format[String]
  }

  sealed abstract class TermVectorVariant(override val value: String) extends StringEnumEntry
  object TermVectorVariant {
    implicit def tvvJson: Format[TermVectorVariant] =
      EnumeratumUtil.valueEnumEntryFormat( TermVectorVariants )
  }
  object TermVectorVariants extends StringEnum[TermVectorVariant] {
    case object No extends TermVectorVariant( "no" )
    case object Yes extends TermVectorVariant("yes")
    case object WithOffsets extends TermVectorVariant("with_offsets")
    case object WithPositions extends TermVectorVariant("with_positions")
    case object WithPositionsOffsets extends TermVectorVariant("with_positions_offsets")

    def default: TermVectorVariant = No
    override def values = findValues
  }


  sealed abstract class DocFieldType(override val value: String) extends StringEnumEntry
  object DocFieldType {
    implicit def dftJson: Format[DocFieldType] =
      EnumeratumUtil.valueEnumEntryFormat( DocFieldTypes )
  }
  object DocFieldTypes extends StringEnum[DocFieldType] {
    case object Text extends DocFieldType("text")
    case object KeyWord extends DocFieldType("keyword")
    case object Short extends DocFieldType("short")
    case object Integer extends DocFieldType("integer")
    case object Long extends DocFieldType("long")
    case object Float extends DocFieldType("float")
    case object Double extends DocFieldType("double")
    case object Boolean extends DocFieldType("boolean")
    case object Null extends DocFieldType("null")
    case object MultiField extends DocFieldType("multi_field")
    case object Ip extends DocFieldType("ip")
    case object GeoPoint extends DocFieldType("geo_point")
    case object GeoShape extends DocFieldType("geo_shape")
    case object Attachment extends DocFieldType("attachment")
    case object Date extends DocFieldType("date")
    case object Binary extends DocFieldType("binary")
    case object NestedObject extends DocFieldType("nested")
    case object Object extends DocFieldType("object")

    override def values = findValues
  }


  sealed abstract class GeoShapeTree(override val value: String) extends StringEnumEntry
  object GeoShapeTree {
    implicit val gstJson: Format[GeoShapeTree] =
      EnumeratumUtil.valueEnumEntryFormat( GeoShapeTrees )
  }
  object GeoShapeTrees extends StringEnum[GeoShapeTree] {
    case object GeoHash extends GeoShapeTree("geohash")
    case object QuadTree extends GeoShapeTree("quadtree")

    override def values = findValues
  }


  sealed abstract class TokenCharType(override val value: String) extends StringEnumEntry
  object TokenCharType {
    implicit def tctJson: Format[TokenCharType] =
      EnumeratumUtil.valueEnumEntryFormat( TokenCharTypes )
  }
  object TokenCharTypes extends StringEnum[TokenCharType] {
    case object Letter extends TokenCharType("letter")
    case object Digit extends TokenCharType("digit")
    case object WhiteSpace extends TokenCharType("whitespace")
    case object Punctuation extends TokenCharType("punctuation")
    case object Symbol extends TokenCharType("symbol")

    override def values = findValues
  }


  object CharFilter {
    implicit lazy val charFilterJson: OFormat[CharFilter] = (
      Util.typ and
      (__ \ "escaped_tags").formatNullable[Iterable[String]] and
      (__ \ "mappings").formatNullable[Iterable[String]] and
      (__ \ "pattern").formatNullable[String] and
      (__ \ "replacement").formatNullable[String]
    )(apply, unlift(unapply))

    def htmlStrip(escapedTags: Iterable[String]) = apply(
      typ = "html_strip",
      escapedTags = Some(escapedTags),
    )

    def mapping(mappings: Iterable[String]) = apply(
      typ = "mapping",
      mappings = Some(mappings),
    )

    def patternReplace(pattern: String, replacement: String) = apply(
      typ = "pattern_replace",
      pattern = Some(pattern),
      replacement = Some(replacement),
    )

  }
  final case class CharFilter private (
                                        typ          : String,
                                        // type = html_strip
                                        escapedTags  : Option[Iterable[String]]  = None,
                                        // t = mapping
                                        mappings     : Option[Iterable[String]]  = None,
                                        // t = pattern_replace
                                        pattern      : Option[String]            = None,
                                        replacement  : Option[String]            = None,
                                      )


  object Analyzer {

    def custom(
                charFilters: Seq[String] = Nil,
                tokenizer: String,
                filters : Seq[String] = Nil,
              ) = apply(
      charFilters = charFilters,
      tokenizer   = Some(tokenizer),
      filters     = filters,
    )

    implicit lazy val analyzerJson: OFormat[Analyzer] = (
      (__ \ "char_filters").formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { chFs => Option.when(chFs.nonEmpty)(chFs) }
        ) and
      (__ \ "tokenizer").formatNullable[String] and
      (__ \ "filters").formatNullable[Seq[String]]
        .inmap[Seq[String]](
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { chFs => Option.when(chFs.nonEmpty)(chFs) }
        )
    )(apply, unlift(unapply))

  }
  final case class Analyzer private(
                                     // analyzer.type = custom
                                     charFilters  : Seq[String]       = Nil,
                                     tokenizer    : Option[String]    = None,
                                     filters      : Seq[String]       = Nil,
                                   )



  object Tokenizer {

    def standard(maxTokenLen: Option[Int] = None) = apply(
      typ           = "standard",
      maxTokenLen   = maxTokenLen,
    )

    def nGram( minGram    : Int,
               maxGram    : Int,
               tokenChars : Seq[TokenCharType] = Seq.empty
             ): Tokenizer = apply(
      typ         = "nGram",
      minGram     = Some(minGram),
      maxGram     = Some(maxGram),
      tokenChars  = tokenChars,
    )

    def keyWord( bufferSize  : Option[Int] = None ) = apply(
      typ         = "keyword",
      bufferSize  = bufferSize,
    )

    implicit lazy val tokenizerJson: OFormat[Tokenizer] = (
      Util.typ and
      // standard
      (__ \ "max_token_length").formatNullable[Int] and
      // ngram
      (__ \ "min_gram").formatNullable[Int] and
      (__ \ "max_gram").formatNullable[Int] and
      (__ \ "token_chars").formatNullable[Seq[TokenCharType]]
        .inmap[Seq[TokenCharType]](
          EmptyUtil.opt2ImplEmptyF( Nil ),
          { chFs => Option.when(chFs.nonEmpty)(chFs) }
        ) and
      // keyword
      (__ \ "buffer_size").formatNullable[Int]
    )(apply, unlift(unapply))

  }
  final case class Tokenizer private (
                                       typ            : String,
                                       // standard
                                       maxTokenLen    : Option[Int]           = None,
                                       // ngram
                                       minGram        : Option[Int]           = None,
                                       maxGram        : Option[Int]           = None,
                                       tokenChars     : Seq[TokenCharType]    = Nil,
                                       bufferSize     : Option[Int]           = None,
                                     )



  object Filter {

    implicit lazy val filterJson: OFormat[Filter] = (
      Util.typ and
      // stopwords
      (__ \ "stopwords").formatNullable[String] and
      // word delimiter
      (__ \ "preserve_original").formatNullable[Boolean] and
      // stemmer
      (__ \ "language").formatNullable[String] and
      // length
      (__ \ "min").formatNullable[Int] and
      (__ \ "max").formatNullable[Int] and
      // edgeNGram
      (__ \ "min_gram").formatNullable[Int] and
      (__ \ "max_gram").formatNullable[Int] and
      (__ \ "side").formatNullable[String]
    )(apply, unlift(unapply))

    def stopWords(
                   stopWords: String,    // = english, russian, etc
                 ) = apply(
      typ = "stopwords",
      stopWords = Some( stopWords ),
    )

    def wordDelimiter( preserveOriginal: Boolean ) = apply(
      typ = "word_delimiter",
      preserveOriginal = Some( preserveOriginal ),
    )

    def stemmer( language: String ) = apply(
      typ = "stemmer",
      language = Some( language ),
    )

    def lowerCase = apply(
      typ = "lowercase",
    )

    def length(min: Int = 0,
               max: Int,
              ) = apply(
      typ = "length",
      min = Some(min),
      max = Some(max),
    )

    def standard = apply(
      typ = "standard",
    )

    def edgeNGram( minGram : Int,
                   maxGram : Int,
                   side : String = "front"
                 ) = apply(
      typ = "edgeNGram",
      minGram = Some(minGram),
      maxGram = Some(maxGram),
      side    = Some(side),
    )

    def nGram( minGram: Int,
               maxGram: Int,
             ) = apply(
      typ = "nGram",
      minGram = Some(minGram),
      maxGram = Some(maxGram),
    )

  }
  final case class Filter private(
                                   typ                : String,
                                   // stopwords
                                   stopWords          : Option[String]          = None,
                                   // word delimiter
                                   preserveOriginal   : Option[Boolean]         = None,
                                   // stemmer
                                   language           : Option[String]          = None,
                                   // length
                                   min                : Option[Int]             = None,
                                   max                : Option[Int]             = None,
                                   // edgeNGram
                                   minGram            : Option[Int]             = None,
                                   maxGram            : Option[Int]             = None,
                                   side               : Option[String]          = None,
                                 )


  object IndexSettings {
    implicit lazy val indexSettingsJson: OFormat[IndexSettings] = (
      (__ \ "number_of_shards").formatNullable[Int] and
      (__ \ "number_of_replicas").formatNullable[Int] and
      (__ \ "analysis").format[IndexSettingsAnalysis]
    )(apply, unlift(unapply))
  }
  final case class IndexSettings(
                                  shards        : Option[Int],
                                  replicas      : Option[Int],
                                  analysis      : IndexSettingsAnalysis   = IndexSettingsAnalysis(),
                                )


  object IndexSettingsAnalysis {
    implicit lazy val indexSettingsAnalysisJson: OFormat[IndexSettingsAnalysis] = (
      (__ \ "char_filter").formatNullable[Map[String, CharFilter]]
        .inmap[Map[String, CharFilter]](
          EmptyUtil.opt2ImplEmptyF( Map.empty ),
          ts => Option.when(ts.nonEmpty)(ts)
        ) and
      (__ \ "analyzer").formatNullable[Map[String, Analyzer]]
        .inmap[Map[String, Analyzer]](
          EmptyUtil.opt2ImplEmptyF( Map.empty ),
          ts => Option.when(ts.nonEmpty)(ts)
        ) and
      (__ \ "tokenizer").formatNullable[Map[String, Tokenizer]]
        .inmap[Map[String, Tokenizer]](
          EmptyUtil.opt2ImplEmptyF( Map.empty ),
          ts => Option.when(ts.nonEmpty)(ts)
        ) and
      (__ \ "filter").formatNullable[Map[String, Filter]]
        .inmap[Map[String, Filter]](
          EmptyUtil.opt2ImplEmptyF( Map.empty ),
          ts => Option.when(ts.nonEmpty)(ts)
        )
    )(apply, unlift(unapply))
  }
  final case class IndexSettingsAnalysis(
                                          charFilters         : Map[String, CharFilter]  = Map.empty,
                                          analyzers           : Map[String, Analyzer]    = Map.empty,
                                          tokenizers          : Map[String, Tokenizer]   = Map.empty,
                                          filters             : Map[String, Filter]      = Map.empty,
                                        )
    extends EmptyProduct



  // -----------------------------------------------------------------------------
  // Поля маппингов индекса
  // Поля описываются через Json.obj( "asdasd" -> FieldText(), .., ...) => JsObject
  // -----------------------------------------------------------------------------

  /** Общая утиль для сборки полей. */
  private object FieldsUtil {

    lazy val docFieldType =
      (__ \ "type").format[DocFieldType]

    lazy val indexName =
      (__ \ "index_name").formatNullable[String]

    lazy val index =
      (__ \ "index").formatNullable[Boolean]

    lazy val store =
      (__ \ "store").formatNullable[Boolean]

    lazy val fieldJson = {
      docFieldType and
      indexName and
      store and
      index and
      (__ \ "null_value").formatNullable[String] and
      (__ \ "boost").formatNullable[Double] and
      (__ \ "fields").formatNullable[JsObject] and
      (__ \ "doc_values").formatNullable[Boolean]
    }

    lazy val ignoreAbove =
      (__ \ "ignore_above").formatNullable[Boolean]

    lazy val precisionStep =
      (__ \ "precision_step").formatNullable[Int]

    lazy val ignoreMalformed =
      (__ \ "ignore_malformed").formatNullable[Boolean]

    lazy val enabled =
      (__ \ "enabled").formatNullable[Boolean]

    lazy val properties =
      (__ \ "properties").formatNullable[JsObject]

  }


  object FText {
    implicit val fieldTextJson: OFormat[FText] = (
      FieldsUtil.fieldJson and
      (__ \ "term_vector").formatNullable[TermVectorVariant] and
      (__ \ "omit_norms").formatNullable[Boolean] and
      (__ \ "index_options").formatNullable[String] and
      (__ \ "analyzer").formatNullable[String] and
      (__ \ "search_analyzer").formatNullable[String] and
      FieldsUtil.ignoreAbove and
      (__ \ "position_offset_gap").formatNullable[Int]
    )(apply, unlift(unapply))

    lazy val indexedJs = Json.toJson( apply( index = someTrue ) )
    lazy val notIndexedJs = Json.toJsObject( apply(index = someFalse) )

  }
  final case class FText(
                          typ                  : DocFieldType                  = DocFieldTypes.Text,
                          indexName            : Option[String]                = None,
                          store                : Option[Boolean]               = None,
                          index                : Option[Boolean]               = None,
                          nullValue            : Option[String]                = None,
                          boost                : Option[Double]                = None,
                          fields               : Option[JsObject]              = None,
                          docValues            : Option[Boolean]               = None,
                          // field-text:
                          termVector           : Option[TermVectorVariant]     = None,
                          omitNorms            : Option[Boolean]               = None,
                          indexOptions         : Option[String]                = None,
                          analyzer             : Option[String]                = None,
                          searchAnalyzer       : Option[String]                = None,
                          ignoreAbove          : Option[Boolean]               = None,
                          positionOffsetGap    : Option[Int]                   = None,
                        )



  object FKeyWord {
    implicit val fieldKeyWordJson: OFormat[FKeyWord] = (
      FieldsUtil.fieldJson and
      FieldsUtil.ignoreAbove
    )(apply, unlift(unapply))

    lazy val indexedJs = Json.toJson( apply( index = someTrue ) )
    lazy val notIndexedJs = Json.toJsObject( apply(index = someFalse) )
  }
  case class FKeyWord(
                       typ                  : DocFieldType                  = DocFieldTypes.KeyWord,
                       indexName            : Option[String]                = None,
                       store                : Option[Boolean]               = None,
                       index                : Option[Boolean]               = None,
                       nullValue            : Option[String]                = None,
                       boost                : Option[Double]                = None,
                       fields               : Option[JsObject]              = None,
                       docValues            : Option[Boolean]               = None,
                       // field keyword
                       ignoreAbove          : Option[Boolean]               = None,
                     )


  object FIp {
    implicit val fieldIpJson: OFormat[FIp] = (
      FieldsUtil.fieldJson and
      FieldsUtil.precisionStep
    )(apply, unlift(unapply))

    lazy val indexedJs = Json.toJson( apply( index = someTrue ) )
    lazy val notIndexedJs = Json.toJsObject( apply(index = someFalse) )
  }
  case class FIp(
                  typ                  : DocFieldType                  = DocFieldTypes.Ip,
                  indexName            : Option[String]                = None,
                  store                : Option[Boolean]               = None,
                  index                : Option[Boolean]               = None,
                  nullValue            : Option[String]                = None,
                  boost                : Option[Double]                = None,
                  fields               : Option[JsObject]              = None,
                  docValues            : Option[Boolean]               = None,
                  // field ip:
                  precisionStep        : Option[Int]                   = None,
                )


  object FNumber {
    implicit val fieldNumberJson: OFormat[FNumber] = (
      FieldsUtil.fieldJson and
      FieldsUtil.precisionStep and
      FieldsUtil.ignoreMalformed
    )(apply, unlift(unapply))
  }
  case class FNumber(
                      typ                  : DocFieldType,    // int | short | long | float | double | ...
                      indexName            : Option[String]                = None,
                      store                : Option[Boolean]               = None,
                      index                : Option[Boolean]               = None,
                      nullValue            : Option[String]                = None,
                      boost                : Option[Double]                = None,
                      fields               : Option[JsObject]              = None,
                      docValues            : Option[Boolean]               = None,
                      // field number
                      precisionStep        : Option[Int]                   = None,
                      ignoreMalformed      : Option[Boolean]               = None,
                    )


  object FDate {
    implicit val fieldDateJson: OFormat[FDate] = (
      FieldsUtil.fieldJson and
      FieldsUtil.precisionStep and
      FieldsUtil.ignoreMalformed
    )(apply, unlift(unapply))

    lazy val indexedJs = Json.toJsObject( apply(index = someTrue) )
    lazy val notIndexedJs = Json.toJsObject( apply(index = someFalse) )
  }
  case class FDate(
                    typ                  : DocFieldType                  = DocFieldTypes.Date,
                    indexName            : Option[String]                = None,
                    store                : Option[Boolean]               = None,
                    index                : Option[Boolean]               = None,
                    nullValue            : Option[String]                = None,
                    boost                : Option[Double]                = None,
                    fields               : Option[JsObject]              = None,
                    docValues            : Option[Boolean]               = None,
                    // field date
                    precisionStep        : Option[Int]                   = None,
                    ignoreMalformed      : Option[Boolean]               = None,
                  )


  object FBoolean {
    implicit val fieldBooleanJson: OFormat[FBoolean] =
      FieldsUtil.fieldJson(apply, unlift(unapply))

    lazy val indexedJs = Json.toJsObject( apply(index = someTrue) )
    lazy val notIndexedJs = Json.toJsObject( apply(index = someFalse) )
  }
  case class FBoolean(
                       typ                  : DocFieldType                  = DocFieldTypes.Boolean,
                       indexName            : Option[String]                = None,
                       store                : Option[Boolean]               = None,
                       index                : Option[Boolean]               = None,
                       nullValue            : Option[String]                = None,
                       boost                : Option[Double]                = None,
                       fields               : Option[JsObject]              = None,
                       docValues            : Option[Boolean]               = None,
                     )


  object FBinary {
    implicit val fieldBinaryJson: OFormat[FBinary] = (
      FieldsUtil.indexName and
      FieldsUtil.docFieldType
    )(apply, unlift(unapply))
  }
  case class FBinary(
                      indexName   : Option[String]    = None,
                      typ         : DocFieldType      = DocFieldTypes.Binary,
                    )


  object FObject {
    implicit val fieldObjectJson: OFormat[FObject] = (
      FieldsUtil.docFieldType and
      FieldsUtil.enabled and
      FieldsUtil.properties and
      (__ \ "include_in_parent").formatNullable[Boolean] and
      (__ \ "include_in_root").formatNullable[Boolean]
    )(apply, unlift(unapply))

    def disabled: FObject = apply(
      typ         = DocFieldTypes.Object,
      enabled     = someFalse,
    )

    def plain( esProps: IEsMappingProps ): FObject =
      plain( esProps.esMappingProps(dsl) )
    def plain( properties: JsObject ): FObject = apply(
      typ         = DocFieldTypes.Object,
      enabled     = someTrue,
      properties  = Some( properties ),
    )

    def nested( properties        : JsObject,
                enabled           : Option[Boolean]     = someTrue,
                includeInParent   : Option[Boolean]     = None,
                includeInRoot     : Option[Boolean]     = None,
              ): FObject = apply(
      typ             = DocFieldTypes.NestedObject,
      enabled         = enabled,
      properties      = Some(properties),
      includeInParent = includeInParent,
      includeInRoot   = includeInRoot,
    )

  }
  case class FObject(
                      typ               : DocFieldType        = DocFieldTypes.Object,
                      enabled           : Option[Boolean],
                      properties        : Option[JsObject]    = None,
                      // nested:
                      includeInParent   : Option[Boolean]     = None,
                      includeInRoot     : Option[Boolean]     = None,
                    )


  object FGeoPoint {
    implicit val fieldGeoPointJson: OFormat[FGeoPoint] = (
      FieldsUtil.fieldJson and
      FieldsUtil.ignoreMalformed
    )(apply, unlift(unapply))

    lazy val indexedJs = Json.toJsObject( apply(index = someTrue) )
    lazy val notIndexedJs = Json.toJsObject( apply(index = someFalse) )
  }
  case class FGeoPoint(
                        typ                  : DocFieldType                  = DocFieldTypes.GeoPoint,
                        indexName            : Option[String]                = None,
                        store                : Option[Boolean]               = None,
                        index                : Option[Boolean]               = None,
                        nullValue            : Option[String]                = None,
                        boost                : Option[Double]                = None,
                        fields               : Option[JsObject]              = None,
                        docValues            : Option[Boolean]               = None,
                        // GeoPoint:
                        ignoreMalformed      : Option[Boolean]               = None,
                      )


  object FGeoShape {
    implicit val fieldGeoShapeJson: OFormat[FGeoShape] = (
      (__ \ "tree").formatNullable[GeoShapeTree] and
      (__ \ "precision").formatNullable[String] and
      (__ \ "tree_levels").formatNullable[Int] and
      (__ \ "distance_error_pct").formatNullable[Double] and
      FieldsUtil.docFieldType
    )(apply, unlift(unapply))
  }
  case class FGeoShape(
                        // GeoShape
                        tree                 : Option[GeoShapeTree]          = None,
                        precision            : Option[String]                = None, // "10m"
                        treeLevels           : Option[Int]                   = None,
                        distanceErrorPct     : Option[Double]                = None,
                        typ                  : DocFieldType                  = DocFieldTypes.GeoShape,
                      )


  object FId {
    implicit val fieldIdJson: OFormat[FId] = (
      FieldsUtil.index and
      FieldsUtil.store
    )(apply, unlift(unapply))
  }
  case class FId(
                  index   : Option[Boolean]     = None,
                  store   : Option[Boolean]     = None,
                )


  object FSource {
    implicit val fieldSourceJson: OFormat[FSource] = {
      FieldsUtil.enabled
        .inmap[FSource](apply, _.enabled)
    }
  }
  case class FSource(
                      enabled   : Option[Boolean]  = None,
                    )


  object FParent {
    implicit val fieldParentJson: OFormat[FParent] =
      Util.typ
        .inmap[FParent](apply, unlift(unapply))
  }
  case class FParent(typ: String)


  object FRouting {
    implicit val fieldRoutingJson: OFormat[FRouting] = (
      (__ \ "required").formatNullable[Boolean] and
      FieldsUtil.store and
      FieldsUtil.index and
      (__ \ "path").formatNullable[String]
    )(apply, unlift(unapply))
  }
  case class FRouting(
                       required     : Option[Boolean]   = None,
                       store        : Option[Boolean]   = None,
                       index        : Option[Boolean]   = None,
                       path         : Option[String]    = None,
                     )



  object DynTemplate {
    implicit val dynTemplateJson: OFormat[DynTemplate] = (
      (__ \ "match").formatNullable[String] and
      (__ \ "match_mapping_type").format[String] and
      (__ \ "mapping").formatNullable[String]
    )(apply, unlift(unapply))
  }
  /** Dynamic templates - механизм задания автоматики при автоматическом добавлении новых полей в маппинг.
    *
    * @see [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-root-object-type.html#_dynamic_templates]]
    */
  case class DynTemplate(
                          nameMatch         : Option[String],
                          matchMappingType  : String = "{dynamic_type}",
                          mapping           : Option[String],
                        )


  object IndexMapping {
    implicit val indexMappingJson: OFormat[IndexMapping] = (
      FieldsUtil.properties and
      (__ \ "_id").formatNullable[FId] and
      (__ \ "_source").formatNullable[FSource] and
      (__ \ "_parent").formatNullable[FParent] and
      (__ \ "_routing").formatNullable[FRouting] and
      (__ \ "dynamic_templates").formatNullable[Map[String, DynTemplate]]
        .inmap [Map[String, DynTemplate]] (
          EmptyUtil.opt2ImplEmptyF( Map.empty ),
          dts => Option.when(dts.nonEmpty)(dts),
        )
    )(apply, unlift(unapply))
  }
  case class IndexMapping(
                           // TODO typ: ES-6.x: удалить при/после апдейта.
                           properties       : Option[JsObject],
                           id               : Option[FId]          = None,
                           source           : Option[FSource]      = None,
                           parent           : Option[FParent]      = None,
                           routing          : Option[FRouting]     = None,
                           dynTemplates     : Map[String, DynTemplate]    = Map.empty,
                         )


  implicit class SubModelsOpsExt( subModels: IterableOnce[(String, IEsMappingProps)] ) {

    def esSubModelsJsObjects(nested: Boolean): JsObject = {
      val typ =
        if (nested) DocFieldTypes.NestedObject
        else DocFieldTypes.Object

      subModels
        .iterator
        .map { case (k, m) =>
          Json.obj(
            k -> FObject.apply(
              typ = typ,
              enabled = someTrue,
              properties = Some( m.esMappingProps(dsl) ),
            )
          )
        }
        .reduce(_ ++ _)
    }

  }

}


object MappingDsl {
  /** Для ручного импорта - завёрнуто в под-объект. */
  object Implicits {
    implicit def mkNewDsl: MappingDsl =
      new MappingDsl
  }
}

/** Интерфейс для сборки пропертисов. */
trait IEsMappingProps {
  def esMappingProps(implicit dsl: MappingDsl): JsObject
}
