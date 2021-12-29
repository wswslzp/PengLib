package object Interpolate {
  case class SquareGrid(pointPerDim: Int) {
    import scala.math.pow
    def totalPoint = pointPerDim * pointPerDim
  }
  case class Coordinate(grid: SquareGrid) {
    def indexToCoordinate(index: Int): List[Int] = {
      require(index < grid.totalPoint, s"Index greater than the grid size ${grid.totalPoint}")
      val ret = List.newBuilder[Int]
      val x = index % grid.pointPerDim
      val y = index / grid.pointPerDim
      ret += x; ret += y
      ret.result()
    }
  }
}
