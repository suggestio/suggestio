package io.suggest

import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.locationtech.spatial4j.shape.Shape

package object geo {

  /** Elasticsearch-6.x+ requires generic-types for ShapeBuilder.
    * Here is workaround, to safely ignore all these generics. */
  type AnyEsShapeBuilder_t = ShapeBuilder[T, E] forSome { type T <: Shape; type E <: ShapeBuilder[T, E] }

}
