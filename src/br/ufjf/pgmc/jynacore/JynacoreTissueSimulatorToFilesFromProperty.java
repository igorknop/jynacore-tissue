/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgmc.jynacore;

import br.ufjf.mmc.jynacore.JynaSimulableModel;
import br.ufjf.mmc.jynacore.JynaSimulation;
import br.ufjf.mmc.jynacore.JynaSimulationData;
import br.ufjf.mmc.jynacore.JynaSimulationProfile;
import br.ufjf.mmc.jynacore.JynaValued;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationData;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationProfile;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstanceStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstanceStorerJDOM;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceRK4Method;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Properties;

public class JynacoreTissueSimulatorToFilesFromProperty {

    public static void main(String[] args) throws Exception {
        JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
        JynaSimulationProfile profile = new DefaultSimulationProfile();
        JynaSimulableModel instance;// = new DefaultMetaModelInstance();
        DefaultSimulationData data = new DefaultSimulationData();

        MetaModelInstanceStorer storer = new DefaultMetaModelInstanceStorerJDOM();
        String modelFile;
        String matchExps;
        String filePrefix;

        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(args[0]));
        } catch (Exception e) {
            System.out.println("Usage: app <simulation profile property file>\n");
            return;
        }
        modelFile = properties.getProperty("modelInstanceFile");
        instance = storer.loadFromFile(new File(modelFile));
        String simMethod = properties.getProperty("method", "rk4");
        if (simMethod.equals("rk4")) {
            simulation.setMethod(new DefaultMetaModelInstanceRK4Method());
        } else {
            simulation.setMethod(new DefaultMetaModelInstanceEulerMethod());
        }
        filePrefix = properties.getProperty("dataFilePrefix", modelFile);
        profile.setInitialTime(Double.valueOf(properties.getProperty("initialTime", "0.0")));
        profile.setFinalTime(Double.valueOf(properties.getProperty("finalTime", "1.0")));
        profile.setTimeLimits(Integer.valueOf(properties.getProperty("iterations", "100")), Double.valueOf(properties.getProperty("finalTime", "1.0")));
        int skip = Integer.valueOf(properties.getProperty("registerSkip", "1"));

        simulation.setProfile(profile);
        data.removeAll();
        data.clearAll();

        matchExps = properties.getProperty("propertyName", "Value");
        String[] exps = matchExps.split(";");
        for (JynaValued jv : instance.getAllJynaValued()) {
            ClassInstanceItem cii = (ClassInstanceItem) jv;

            for (String e : exps) {
                if (cii.getName().matches(e)) {
                    data.add(cii.getClassInstance().getName() + "." + cii.getName(), jv);
                    System.out.println(cii.getName());
                }
            }
        }

        simulation.setModel(instance);
        simulation.setSimulationData((JynaSimulationData) data);
        simulation.reset();

        data.register(0.0);
        runSimulation(simulation, skip);

        File file = new File(filePrefix + ".dat");
        FileWriter fw = new FileWriter(file);
        fw.write(data.toString());

    }

    private static void runSimulation(JynaSimulation simulation, int skip) throws Exception {
        int steps = simulation.getProfile().getTimeSteps();

        System.out.println("Simulating with " + simulation.getProfile().getTimeSteps() + " iterations. Interval " + simulation.getProfile().getTimeInterval() + " to " + simulation.getProfile().getFinalTime());
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
