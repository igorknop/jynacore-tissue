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
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstanceStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstanceStorerJDOM;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author igor
 */
public class JynacoreTissueSimulatorToFiles2 {

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
      List<String> propName;
      String filePrefix;
      Double initialTime;
      Double finalTime;
      Integer steps;
      Integer skip;
      try {
         initialTime = Double.parseDouble(args[0]);
         finalTime = Double.parseDouble(args[1]);
         steps = Integer.parseInt(args[2]);
         skip = Integer.parseInt(args[3]);
         modelFile = args[4];
         propName = Arrays.asList(args[5].split(";"));
         filePrefix = args[6];
         instance = storer.loadFromFile(new File(modelFile));
      } catch (Exception e) {
         System.out.println("Usage: " + args[0] + "<initial time> <final time> <steps> <register step><metamodelinstance> <properties> <output file>\n");
         return;
      }
      profile.setTimeLimits(initialTime, finalTime, steps);

      simulation.setMethod(method);
      simulation.setProfile(profile);
      data.removeAll();
      data.clearAll();

      for (JynaValued jv : instance.getAllJynaValued()) {
         ClassInstanceItem cii = (ClassInstanceItem) jv;
         if (propName.contains(cii.getName())) {
            data.add(cii.getClassInstance().getName() + "." + cii.getName(), jv);
         }
      }

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
