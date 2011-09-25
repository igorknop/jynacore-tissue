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
import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceInvalidLinkException;
import br.ufjf.mmc.jynacore.metamodel.impl.JDOMMetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;

/**
 *
 * @author igor
 */
public class JynacoreTissueThreads {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
    JynaSimulationProfile profile = new DefaultSimulationProfile();
    JynaSimulationMethod method = new DefaultMetaModelInstanceEulerMethodThreads2();
    JynaSimulableModel instance = new DefaultMetaModelInstance();
    DefaultSimulationData data = new DefaultSimulationData();

    MetaModelStorer storer = new JDOMMetaModelStorer();
    MetaModel metamodel = storer.loadFromFile(new File("planar.jymm"));
    ((MetaModelInstance) instance).setMetaModel(metamodel);
    profile.setInitialTime(0.0);
    profile.setFinalTime(0.03);
    profile.setTimeSteps(2);
    int skip = 1;

    simulation.setProfile(profile);
    simulation.setMethod(method);
    //data.removeAll();
    data.clearAll();

    int rows = 5;
    int cols = 5;
    MetaModelInstance mmi = createCells(instance, rows, cols, data);
    connectCells(rows, cols, mmi);

    simulation.setModel(instance);
    simulation.setSimulationData(
            (JynaSimulationData) data);
    simulation.reset();
    data.register(0.0);
    runSimulation(simulation, skip);

    //System.out.println(data.getWatchedNames());
    System.out.println(data);
  }

  private static void runSimulation(JynaSimulation simulation, int skip) throws Exception {
    //simulation.run();
    int steps = simulation.getProfile().getTimeSteps();

    //System.out.println("Simulating with "+steps+" iterations.");
    for (int i = 0;
            i < steps;
            i++) {
      simulation.step();
      if (i % skip == 0) {
        simulation.register();
      }
    }
    //System.out.println("Simulating done!");
  }

  private static void connectCells(int rows, int cols, MetaModelInstance mmi) throws MetaModelInstanceInvalidLinkException {
    //System.out.println("Creating meshes...");
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        ClassInstance eci = mmi.getClassInstances().get("cell[" + i + "," + j + "]");
        eci.setLink("east", "cell[" + i + "," + ((j == cols-1) ? j : j + 1) + "]");
        eci.setLink("west", "cell[" + i + "," + ((j == 0) ? j : j - 1) + "]");
        eci.setLink("north", "cell[" + ((i == 0) ? i : i - 1) + "," + j + "]");
        eci.setLink("south", "cell[" + ((i == rows-1) ? i : i + 1) + "," + j + "]");
      }
    }
  }

  private static MetaModelInstance createCells(JynaSimulableModel instance, int rows, int cols, DefaultSimulationData data) throws Exception {
    instance.setName("Tissue");
    MetaModelInstance mmi = (MetaModelInstance) instance;
    //System.out.println("Creating "+(rows*cols)+"instances...");
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        ClassInstance ci = mmi.addNewClassInstance("cell[" + i + "," + j + "]", "Cell");
        ci.setProperty("InitialValue", (i == 0 && j == 0) ? 100.0 : 0.0);
        data.add("cell[" + i + "," + j + "]", (JynaValued) ci.get("Value"));
      }
    }
    return mmi;
  }
}
