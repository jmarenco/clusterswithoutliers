package colgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import general.Instance;

public class Population
{
	private ArrayList<Individual> _individuals;
	
	private Instance _instance;
	private MasterPacking _master;
	
	private int _size = 100;
	private int _mutatedPerIteration = 20;
	private int _recombinedPerIteration = 20;
	private int _removesPerIteration = 60;
	private int _maxIterations = 100;
	
	private int _iteration;
	
	private static Random _random = new Random(0);
	
	public Population(Instance instance, MasterPacking master)
	{
		_instance = instance;
		_master = master;
	}
	
	public void execute()
	{
		_iteration = 0;
		generateIndividuals();
		
		while( !satisfactory() )
		{
			mutateIndividuals();
			recombineIndividuals();
			removeWorst();
			addIndividuals();
			
			_iteration++;
		}
	}

	private void generateIndividuals()
	{
		_individuals = new ArrayList<Individual>(_size);
		
		for(int i=0; i<_size; ++i)
			_individuals.add( Individual.random(_instance, _master) );
	}

	private boolean satisfactory()
	{
		return _iteration == _maxIterations;
	}

	private void mutateIndividuals()
	{
		for(int j=0; j<_mutatedPerIteration; ++j)
			randomIndividual().mutate();
	}

	private void recombineIndividuals()
	{
		for(int j=0; j<_recombinedPerIteration; ++j)
		{
			Individual parent1 = randomIndividual();
			Individual parent2 = randomIndividual();

			for(Individual individual: parent1.recombine(parent2))
				_individuals.add(individual);
		}
	}

	private void removeWorst()
	{
		Collections.sort(_individuals);
		Collections.reverse(_individuals); 

		for(int j=0; j<_removesPerIteration; ++j)
			_individuals.remove(_individuals.size()-1);
	}

	private void addIndividuals()
	{
		while( _individuals.size() < _size )
			_individuals.add( Individual.random(_instance, _master) );
	}

	public Individual bestIndividual()
	{
		return Collections.max(_individuals);
	}

	public Individual worstIndividual()
	{
		return Collections.min(_individuals);
	}
	
	public double averageFitness()
	{
		double sum = 0;
		for(Individual individual: _individuals)
			sum += individual.fitness();
		
		return sum / _individuals.size();
	}

	private Individual randomIndividual()
	{
		int i = _random.nextInt(_individuals.size());
		return _individuals.get(i);
	}

	public int getIteration()
	{
		return _iteration;
	}
}