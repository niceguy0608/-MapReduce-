package com;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

public class childparents
{
    public static class MyMapper extends Mapper<LongWritable, Text, Text, Text>
    {
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
        {
            String[] names = value.toString().split(",");
            
            // 提取父子/父女/母子/母女关系
            String child = names[0].trim();
            String parent = names[1].trim();

            // 发送键值对，以child作为键，以parent和关系类型作为值
            context.write(new Text(child), new Text(parent + ",child-parent"));

            // 发送键值对，以parent作为键，以child和关系类型作为值
            context.write(new Text(parent), new Text(child + ",parent-child"));
        }
    }
    
    public static class MyReducer extends Reducer<Text, Text, Text, Text>
    {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
        {
            List<String> parents = new ArrayList<String>();
            List<String> children = new ArrayList<String>();
            
            // 将父母和子女分别存储到不同的列表中
            for (Text value : values)
            {
                String[] fields = value.toString().split(",");
                String name = fields[0].trim();
                String relation = fields[1].trim();

                if (relation.equals("parent-child"))
                {
                    children.add(name);
                }
                else if(relation.equals("child-parent"))
                {
                    parents.add(name);
                }
            }

            // 寻找具有grandchild-grandparent关系的人名组
            for (String child : children)
            {
                for (String parent : parents)
                {
                    // 发送键值对，以parent和child作为键，以grandchild-grandparent关系作为值
                    context.write(new Text(child + ", " + parent), new Text("grandchild-grandparent"));
                }
            }
        }
    }

    //客户端代码，写完交给ResourceManager框架去执行
    public static void main(String[] args) throws Exception
    {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, childparents.class.getSimpleName());
        //打成jar执行
        job.setJarByClass(childparents.class);

        //数据在哪里？
        FileInputFormat.setInputPaths(job, args[0]);

        //使用哪个mapper处理输入的数据？
        job.setMapperClass(MyMapper.class);
        //map输出的数据类型是什么？
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        //使用哪个reducer处理输入的数据？
        job.setReducerClass(MyReducer.class);
        //reduce输出的数据类型是什么？
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        //数据输出到哪里？
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        //交给yarn去执行，直到执行结束才退出本程序
        job.waitForCompletion(true);
    }
}