package fr.inria.astor.test.repair.evaluation.extensionpoints;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.inria.astor.core.faultlocalization.FaultLocalizationStrategy;
import fr.inria.astor.core.faultlocalization.GZoltarFaultLocalization;
import fr.inria.astor.core.loop.spaces.ingredients.AstorIngredientSpace;
import fr.inria.astor.core.loop.spaces.ingredients.IngredientSpace;
import fr.inria.astor.core.loop.spaces.ingredients.scopes.ctscopes.CtPackageIngredientScope;
import fr.inria.main.evolution.PlugInLoader;
/**
 * 
 * @author Matias Martinez
 *
 */
public class PlugInLoaderTest {

	@Test
	public void testNoArgumentInConstr() throws Exception {
		PlugInLoader loader = new PlugInLoader();
		FaultLocalizationStrategy fls =  (FaultLocalizationStrategy) loader.loadPlugin(GZoltarFaultLocalization.class.getCanonicalName(), FaultLocalizationStrategy.class);
		
		assertNotNull(fls);
	
		
	}
	
	@Test
	public void testArguments() throws Exception{
		CtPackageIngredientScope scope = 	(CtPackageIngredientScope) PlugInLoader.loadPlugin(CtPackageIngredientScope.class.getCanonicalName(), //
				AstorIngredientSpace.class,//
				new Class[]{List.class},
				new Object[]{new ArrayList()}
				);
		assertNotNull(scope);
		assertNotNull(scope.getIngredientProcessor());
	}
}
