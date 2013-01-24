Gremlin.defineStep('codeveloper',[Vertex,Pipe], {_().sideEffect{x = it}.out('created').in('created').filter{!x.equals(it)}})

def isMarko(v) {
  v.getProperty("name").equals("marko")
}

