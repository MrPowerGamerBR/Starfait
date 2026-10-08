# Bytecode

WIP: This is VERY WIP, as much as I hate to say "the documentation is the code!"... see the `StarfaitVM` file for reference of how the bytecode works.

## Stack Values

In GameMaker, there are six different types of values that can be on the stack:

* Int32 (4 bytes)
* Int64 (8 bytes)
* Double (8 bytes)
* String (4 bytes, pointer)
* Variable (16 bytes)
* Boolean (4 bytes)

The original runner stores all stack values on a byte array directly, the VM does not know what type of each value on the stack is.

You may be wondering: If the VM does not know the value type, then how does it know how many bytes to pop off the stack when popping values?

And the answer is simple: The GameMaker compiler gives the type for you on each instruction, so it knows how many bytes to pop from the stack.

...but how would we reproduce that on the JVM?

Storing the values as raw bytes would be painful because you would need to have a bunch of "sidecar" arrays just to store metadata. You could wave your hand away and pretend that they don't exist (which is what Butterscotch does), I preferred to do something more faithful to the original runner.

Starfait goes with a mixed approach: It stores all stack values as typed classes on an array, so the stack knows which type they all are, and then when popping values from the stack the VM verifies that the instruction type matches what's the value on the top of the stack, this way anything that goes wrong fails loudly instead of failing silently.