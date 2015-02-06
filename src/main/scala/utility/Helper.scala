package utility

import java.util.ArrayList

/**
 * Created by Konstantin on 04/02/2015.
 */
class Helper {

}

object Helper {

  //  def test(inputArraySize: Integer, segmentSize: Integer): Array[Integer] = {
  //    val avg = inputArraySize / segmentSize
  //    val list: util.ArrayList[Integer] = new util.ArrayList[Integer]()
  //    var last = 0
  //    while (last < inputArraySize){
  //      list.add(last + avg)
  //      last += avg
  //    }
  //
  //    return list.toArray(new Array[Integer](list.size()))
  //  }

  def getRouteSegments(inputArraySize: Integer, segmentSize: Integer): Array[Integer] = {
    val tempResult = splitArrayIntoSegments(inputArraySize, segmentSize)
    tempResult.add(0, 0)
    return tempResult.toArray(new Array[Integer](tempResult.size()))
  }

  def splitArrayIntoSegments(inputArraySize: Integer, segmentSize: Integer, offset: Integer = 0): ArrayList[Integer] = {
    //val leftSize = if (inputArraySize % 2 == 1) inputArraySize / 2 else (inputArraySize - 1) / 2
    val leftSize = Math.round(inputArraySize / 2)
    val rightSize = inputArraySize - leftSize

    var left: ArrayList[Integer] = null
    if (leftSize > segmentSize) {
      left = splitArrayIntoSegments(leftSize, segmentSize, offset)
    } else {
      left = new ArrayList[Integer]()
      left.add(leftSize + offset)
    }

    var right: ArrayList[Integer] = null
    if (rightSize > segmentSize) {
      right = splitArrayIntoSegments(rightSize, segmentSize, leftSize + offset)
    } else {
      right = new ArrayList[Integer]()
      right.add(inputArraySize + offset)
    }

    val result: ArrayList[Integer] = new ArrayList[Integer](left)
    result.addAll(right)
    return result
  }

}
