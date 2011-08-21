/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgmc.jynacore;

import br.ufjf.mmc.jynacore.JynaSimulableModel;
import br.ufjf.mmc.jynacore.JynaSimulation;
import br.ufjf.mmc.jynacore.JynaSimulationData;
import br.ufjf.mmc.jynacore.JynaSimulationMethod;
import br.ufjf.mmc.jynacore.JynaSimulationProfile;
import br.ufjf.mmc.jynacore.JynaValued;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationData;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationProfile;
import br.ufjf.mmc.jynacore.metamodel.MetaModel;
import br.ufjf.mmc.jynacore.metamodel.MetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.impl.JDOMMetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;

/**
 *
 * @author igor
 */
public class JynacoreTissue {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
    JynaSimulationProfile profile = new DefaultSimulationProfile();
    JynaSimulationMethod method = new DefaultMetaModelInstanceEulerMethod();
    JynaSimulableModel instance = new DefaultMetaModelInstance();
    DefaultSimulationData data = new DefaultSimulationData();

    MetaModelStorer storer = new JDOMMetaModelStorer();
    MetaModel metamodel = storer.loadFromFile(new File("planar.jymm"));
    ((MetaModelInstance) instance).setMetaModel(metamodel);
    profile.setInitialTime(0.0);
    profile.setFinalTime(5.0);
    profile.setTimeSteps(500);
    int skip = 10;

    simulation.setProfile(profile);
    simulation.setMethod(method);
    //data.removeAll();
    data.clearAll();

    instance.setName("Tissue");
    MetaModelInstance mmi = (MetaModelInstance) instance;
    int rows = 32;
    int cols = 32;
    System.out.println("Creating "+(rows*cols)+"instances...");
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        ClassInstance ci = mmi.addNewClassInstance("cell[" + i + "," + j + "]", "Cell");
        ci.setProperty("InitialValue", (i == 0 && j == 0) ? 100.0 : 0.0);
        data.add("cell[" + i + "," + j + "]", (JynaValued) ci.get("Value"));
      }
    }
    System.out.println("Creating meshes...");
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        ClassInstance eci = mmi.getClassInstances().get("cell[" + i + "," + j + "]");
        eci.setLink("east", "cell[" + i + "," + ((j == cols-1) ? j : j + 1) + "]");
        eci.setLink("west", "cell[" + i + "," + ((j == 0) ? j : j - 1) + "]");
        eci.setLink("north", "cell[" + ((i == 0) ? i : i - 1) + "," + j + "]");
        eci.setLink("south", "cell[" + ((i == rows-1) ? i : i + 1) + "," + j + "]");
      }
    }

    simulation.setModel(instance);

    simulation.setSimulationData(
            (JynaSimulationData) data);
//    Collection<JynaValued> valueds = instance.getAllJynaValued();
//
//    for (JynaValued jv : valueds) {
//      if (jv.getName().equals("Value")) {
//        data.add(jv);
//      }
//    }
    // data.addAll(model.getAllJynaValued());

    simulation.reset();

    data.register(
            0.0);

    //simulation.run();
    int steps = simulation.getProfile().getTimeSteps();

    System.out.println("Simulating with "+steps+" iterations.");
    for (int i = 0;
            i < steps;
            i++) {
      simulation.step();
      if (i % skip == 0) {
        simulation.register();
      }
    }
    System.out.println("Simulating done!");

    System.out.println(data.getWatchedNames());
    System.out.println(data);
  }
}
