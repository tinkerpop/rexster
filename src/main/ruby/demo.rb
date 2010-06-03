# sudo gem install wreckster

require "wreckster"
graph = Wreckster::Graph.new('http://localhost:8182/')

# List the traversals available in this graph server
graph.traversals.each do |t|
	puts t.name
end

# Execute a traversal with some parameters
traversal = graph.traversals[2]
traversal.find('song.name' => 'SUGAREE').each do |song|
	p song
end
