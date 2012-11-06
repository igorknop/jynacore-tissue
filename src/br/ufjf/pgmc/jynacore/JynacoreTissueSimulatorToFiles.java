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
import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceInvalidLinkException;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstanceStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstanceStorerJDOM;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;
import java.io.FileWriter;

/**
 *
 * @author igor
 */
public class JynacoreTissueSimulatorToFiles {

   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) throws Exception {
      JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
      JynaSimulationProfile profile = new DefaultSimulationProfile();
      JynaSimulationMethod method = new DefaultMetaModelInstanceEulerMethod();
      JynaSimulableModel instance;// = new DefaultMetaModelInstance();
      DefaultSimulationData data = new DefaultSimulationData();

      MetaModelInstanceStorer storer = new DefaultMetaModelInstanceStorerJDOM();
      String modelFile;
      String propName;
      String filePrefix;
      try {
         modelFile = args[0];
         propName = args[1];
         filePrefix = args[2];
      } catch (Exception e) {
         System.out.println("Usage: " + args[0] + "<metamodelinstance> <property> <prefix data files>\n");
         return;
      }
      instance = storer.loadFromFile(new File(modelFile));
      //((MetaModelInstance) instance).setMetaModel(metamodel);
      profile.setInitialTime(0.0);
      profile.setFinalTime(5.0);
      profile.setTimeLimits(10000, 5.0);
      int skip = 10;

      simulation.setMethod(method);
      simulation.setProfile(profile);
      data.removeAll();
      data.clearAll();

      for (JynaValued jv : instance.getAllJynaValued()) {
         ClassInstanceItem cii = (ClassInstanceItem) jv;
         if (cii.getName().equals(propName)) {
            data.add(cii.getClassInstance().getName() + "." + cii.getName(), jv);
         }
      }
      int rows = 5;
      int cols = 5;
      //MetaModelInstance mmi = createCells(instance, rows, cols, data);
      //connectCells(rows, cols, mmi);

      simulation.setModel(instance);
      simulation.setSimulationData(
              (JynaSimulationData) data);
      simulation.reset();

      data.register(0.0);
      runSimulation(simulation, skip);

      try {
         File file = new File(filePrefix + ".dat");
         FileWriter fw = new FileWriter(file);
         fw.write(data.toString());
         fw.close();

      } catch (Exception e) {
         e.printStackTrace();
      }
      //System.out.println(data.getWatchedNames());
      //System.out.println(data);
   
}
private static void runSimulation(JynaSimulation simulation, int skip) throws Exception {
      //simulation.run();
      int steps = simulation.getProfile().getTimeSteps();

      System.out.println("Simulating with "+simulation.getProfile().getTimeSteps()+" iterations. Interval "+simulation.getProfile().getTimeInterval()+" to "+simulation.getProfile().getFinalTime());
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
}
