# Quirks and Miscellaneous Things

* GameMaker: Studio 1.4.x doesn't create `POPZ` statements when you call `exit` inside a `repeat` statement, which causes the VM stack to grow indefinitely. Pop these values manually!
    ```gml
    var i = 0;
    
    repeat(4) {
        if (i == 2) {
            exit; // Oops, the compiler forgot to emit POPZ to pop the repeat counter
        }
        i++;
    }
    ```
* Runtime instances start at the `GEN8.maxRoomInstanceId + 1`. This does create a gap between the room instances and the created instances.
* When a value doesn't match the original runner... *hawk tuah* `DelphiRound` on that thang... and hope for the best!
* While most of the time tiles have the same width/height as their texture, sometimes they aren't and end up rendering garbage, so you'll need to clamp the values to the texture page entry instead of the tile width/height AND adjust the position of the tile.
  * I have no idea how to reproduce that (yet) in GameMaker: Studio, but Undertale has some funky tiles like that.