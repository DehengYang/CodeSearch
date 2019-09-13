#grep -rn "import dale.parser.node.Node;" *

#dale.parser.node.CodeBlock
#cofix.common.util.Pair
#for file in `grep -rn "import dale.parser.node.Node" .`
for file in `grep -rn "dale.parser.node.CodeBlock" .`
do
    if [[ "$file" == ./src/* ]];then
        file_name=`echo $file | cut -d ':' -f 1`
#	echo $file_name
        sed -i 's/dale.parser.node.CodeBlock/dale.search.CodeBlock/g' ${file_name}
    fi
done

