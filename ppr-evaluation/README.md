# Evaluation

This package includes tools to execute the PPR reasoners in an experimental setting.
The experiments aim to compare the impact of rule base compression on reasoning performance.
This is evaluated on two implementations, the one provided by the ppr-prolog module and the other provided by ppr-spin module.

Below is an example of how to run the experimental evaluation for the aemoo1.txt data flow.
The tool is instructed to perform the experiment 1 time.
In the output, `-c` indicates wether to use a compressed or plain rule base.
`-q` is the actual data object that need to be checked for policies, usually the output of the process described in the data flow.

```
MCL260542:ppr-evaluation ed4565$ ./evaluation.sh suites/aemoo1.txt 1
1 1 -  -c 0 -r Prolog -k EVOLVED -d use-cases/AEMOO-1 -q "http://purl.org/datanode/ex/0.2/AEMOO/1#resourceSummary"
Monitoring 54548 (will interrupt in 300 seconds)
............................................................................................................................................................................................................................................................................................................................................................................................................................................................. Done.
2 1 -  -c 1 -r Prolog -k EVOLVED -d use-cases/AEMOO-1 -q "http://purl.org/datanode/ex/0.2/AEMOO/1#resourceSummary"
Monitoring 55926 (will interrupt in 300 seconds)
..................................................................................... Done.
3 1 -  -c 0 -r SPIN -k EVOLVED -d use-cases/AEMOO-1 -q "http://purl.org/datanode/ex/0.2/AEMOO/1#resourceSummary"
Monitoring 56189 (will interrupt in 300 seconds)
.......... Done.
4 1 -  -c 1 -r SPIN -k EVOLVED -d use-cases/AEMOO-1 -q "http://purl.org/datanode/ex/0.2/AEMOO/1#resourceSummary"
Monitoring 56231 (will interrupt in 300 seconds)
.......... Done.
./evaluation.sh: line 55: kill: (56231) - No such process
./evaluation.sh: line 55: kill: (56229) - No such process
```

To run the full experiments simply execute the script without arguments.
Please note that this will probably take hours, as there are 15 use cases, performed 4 times with different parameters (compression and reasoner implementation), and repeated 20 times.
On a Macbook Pro i7 with 16G RAM takes approximately 10 hours.
